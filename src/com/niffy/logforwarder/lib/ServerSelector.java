package com.niffy.logforwarder.lib;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.niffy.logforwarder.lib.logmanagement.ILogManager;

public class ServerSelector extends BaseSelectorThread {
	// ===========================================================
	// Constants
	// ===========================================================
	private final Logger log = LoggerFactory.getLogger(ServerSelector.class);

	// ===========================================================
	// Fields
	// ===========================================================
	protected ServerSocketChannel mTCPChannel;

	// ===========================================================
	// Constructors
	// ===========================================================
	public ServerSelector(final String pName, final InetSocketAddress pAddress, final ILogManager pLogManager)
			throws IOException {
		super(pName, pAddress, pLogManager);
	}

	public ServerSelector(final String pName, final InetSocketAddress pAddress, final int pBufferCapacity,
			final ILogManager pLogManager) throws IOException {
		super(pName, pAddress, pBufferCapacity, pLogManager);
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================
	@Override
	public void run() {
		log.debug("Running TCP Selector Thread");
		while (true) {
			try {
				// Process any pending changes
				synchronized (this.mPendingChanges) {
					Iterator<ChangeRequest> changes = this.mPendingChanges.iterator();
					while (changes.hasNext()) {
						ChangeRequest change = (ChangeRequest) changes.next();
						this.handleChangeRequest(change);
					}
					this.mPendingChanges.clear();
				}

				// Wait for an event one of the registered channels
				this.mSelector.select();

				// Iterate over the set of keys for which events are available
				Iterator<SelectionKey> selectedKeys = this.mSelector.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();

					if (!key.isValid()) {
						continue;
					}

					// Check what event is available and deal with it
					try {
						if (key.isAcceptable()) {
							this.accept(key);
						} else if (key.isReadable()) {
							this.read(key);
						} else if (key.isWritable()) {
							this.write(key);
						}
					} catch (IOException e) {
						Connection con = (Connection) key.attachment();
						if (con != null) {
							log.error("IOException on key operation: {}", con.getAddress(), e);
						} else {
							log.error("IOException on key operation", e);
						}
					}
				}
			} catch (Exception e) {
				log.error("Exception in main loop", e);
			}
		}
	}

	@Override
	protected Selector initSelector() throws IOException {
		Selector found = super.initSelector();

		// Create a new non-blocking server socket channel
		this.mTCPChannel = ServerSocketChannel.open();
		this.mTCPChannel.configureBlocking(false);

		// Bind the server socket to the specified address and port
		this.mTCPChannel.socket().bind(this.mAddress);

		// Register the server socket channel, indicating an interest in
		// accepting new connections
		this.mTCPChannel.register(found, SelectionKey.OP_ACCEPT);

		return found;
	}

	/**
	 * This will send a message to the {@link CommunicationHandler} to inform of
	 * a new client using {@link ITCFlags#NEW_CLIENT_CONNECTED}
	 * 
	 * @see com.niffy.AndEngineLockStepEngine.threads.nio.BaseSelectorThread#accept(java.nio.channels.SelectionKey)
	 */
	@Override
	protected void accept(SelectionKey pKey) throws IOException {
		log.debug("accepting key: {}", pKey);
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) pKey.channel();
		SocketChannel socketChannel = serverSocketChannel.accept();
		Socket socket = socketChannel.socket();
		socketChannel.configureBlocking(false);
		socketChannel.register(this.mSelector, SelectionKey.OP_READ);
		Connection con = new Connection((InetSocketAddress) socket.getRemoteSocketAddress(), socketChannel,
				this.mBufferCapacity);
		this.mChannelMap.put(con.getAddress().getAddress(), con);
		pKey.attach(con);

	}

	@Override
	protected void read(SelectionKey pKey) throws IOException {
		SocketChannel socketChannel;
		InetSocketAddress address;
		Connection con = (Connection) pKey.attachment();
		ByteBuffer buffer;
		if (con != null) {
			socketChannel = con.getSocketChannel();
			address = con.getAddress();
			buffer = con.getBuffer();
		} else {
			socketChannel = (SocketChannel) pKey.channel();
			address = (InetSocketAddress) socketChannel.socket().getRemoteSocketAddress();
			con = new Connection(address, socketChannel, this.mBufferCapacity);
			pKey.attach(con);
			buffer = con.getBuffer();
		}
		buffer.clear();

		// Attempt to read off the channel
		int numRead = -2;
		try {
			numRead = socketChannel.read(buffer);
		} catch (AsynchronousCloseException e) {
			log.error("AsynchronousCloseException: {}", socketChannel.getRemoteAddress(), e);
			this.handleConnectionFailure(pKey, socketChannel, address.getAddress());
			return;
		} catch (NotYetConnectedException e) {
			log.error("NotYetConnectedException: {}", socketChannel.getRemoteAddress(), e);
			this.handleConnectionFailure(pKey, socketChannel, address.getAddress());
			return;
		} catch (ClosedChannelException e) {
			log.error("ClosedChannelException: {}", socketChannel.getRemoteAddress(), e);
			this.handleConnectionFailure(pKey, socketChannel, address.getAddress());
			synchronized (this.mChannelMap) {
				this.mChannelMap.remove(address.getAddress());
			}return;
		} catch (IOException e) {
			log.error("IOException: {}", socketChannel.getRemoteAddress(), e);
			// The remote forcibly closed the connection, cancel
			// the selection key and close the channel.
			this.handleConnectionFailure(pKey, socketChannel, address.getAddress());
			return;
		}

		if (numRead == -1) {
			log.warn("End of stream? shutting down connection");
			this.handleConnectionShutdown(pKey, socketChannel, address.getAddress());
			return;
		}

		byte[] dataIn = new byte[numRead];
		System.arraycopy(buffer.array(), 0, dataIn, 0, numRead);
		this.mLogManager.handle(address, dataIn);
	}

	@Override
	protected void write(SelectionKey pKey) throws IOException, CancelledKeyException {
		SocketChannel socketChannel;
		String connectionIP;
		Connection con = (Connection) pKey.attachment();
		if (con != null) {
			socketChannel = con.getSocketChannel();
			connectionIP = con.getAddress().getAddress().getHostAddress();
		} else {
			socketChannel = (SocketChannel) pKey.channel();
			InetSocketAddress address = (InetSocketAddress) socketChannel.socket().getRemoteSocketAddress();
			connectionIP = address.getAddress().getHostAddress();
			log.debug("Could not get Connection attachment for IP: {}", connectionIP);
		}

		synchronized (this.mPendingData) {
			ArrayList<ByteBuffer> queue = this.mPendingData.get(con.getAddress().getAddress());

			// Write until there's not more data ...
			while (!queue.isEmpty()) {
				ByteBuffer buf = (ByteBuffer) queue.get(0);
				try {
					socketChannel.write(buf);
				} catch (ClosedChannelException e) {
					log.error("ClosedChannelException", e);
				} catch (IOException e) {
					log.error("IOException", e);
				}
				if (buf.remaining() > 0) {
					// ... or the socket's buffer fills up
					break;
				}
				queue.remove(0);
			}

			if (queue.isEmpty()) {
				/* We wrote away all data, so we're no longer interested
				in writing on this socket. Switch back to waiting for
				data. Well switch to OP_READ otherwise we'll write for ever and ever
				 */
				pKey.interestOps(SelectionKey.OP_READ);
			}
		}
	}

	@Override
	protected void handleChangeRequest(ChangeRequest pChangeRequest) {
		switch (pChangeRequest.mType) {
		case ChangeRequest.CHANGEOPS:
			SelectionKey key = pChangeRequest.mChannel.keyFor(this.mSelector);
			if (key == null) {
				log.error("Could not change channel operations for. Null key {} ", pChangeRequest.mChannel.toString());
			} else {
				try {
					key.interestOps(pChangeRequest.mOps);
				} catch (IllegalArgumentException e) {
					log.error("IllegalArgumentException", e);
					/* TODO handle this, clean up pending data and pending changes?
					 * And remove from any collections
					 */
				} catch (CancelledKeyException e) {
					log.error("CancelledKeyException", e);
					/* TODO handle this, clean up pending data and pending changes?
					 * And remove from any collections
					 */
				}
			}
			break;
		case ChangeRequest.REMOVECLIENT:
			try {
				this.handleConnectionShutdown(pChangeRequest.mChannel.keyFor(this.mSelector), pChangeRequest.mChannel,
						pChangeRequest.mAddress);
			} catch (IOException e) {
				log.error("Could not shut downconnection.", e);
			}
			break;
		}
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
