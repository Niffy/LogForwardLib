package com.niffy.logforwarder.lib;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NoConnectionPendingException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.niffy.logforwarder.lib.logmanagement.ILogManager;
import com.niffy.logforwarder.lib.logmanagement.LogRequest;
import com.niffy.logforwarder.lib.messages.IMessage;

public class ClientSelector extends BaseSelectorThread implements IClientSelector {
	// ===========================================================
	// Constants
	// ===========================================================
	private final Logger log = LoggerFactory.getLogger(ClientSelector.class);

	// ===========================================================
	// Fields
	// ===========================================================
	protected int mServerPort = 9889;
	protected List<LogRequest<IMessage>> mPendingRequests = new LinkedList<LogRequest<IMessage>>();

	// ===========================================================
	// Constructors
	// ===========================================================
	public ClientSelector(final String pName, final InetSocketAddress pAddress, final int pServerPort,
			final ILogManager pLogManager) throws IOException {
		super(pName, pAddress, pLogManager);
		this.mServerPort = pServerPort;
	}

	public ClientSelector(final String pName, final InetSocketAddress pAddress, final int pBufferCapacity,
			final int pServerPort, final ILogManager pLogManager) throws IOException {
		super(pName, pAddress, pBufferCapacity, pLogManager);
		this.mServerPort = pServerPort;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces BaseSelectorThread
	// ===========================================================
	@Override
	public void run() {
		log.debug("Running TCP Client Selector Thread");
		while (true) {
			try {
				synchronized (this.mPendingRequests) {
					Iterator<LogRequest<IMessage>> changes = this.mPendingRequests.iterator();
					while (changes.hasNext()) {
						LogRequest<IMessage> request = (LogRequest<IMessage>) changes.next();
						this.handleLogRequest(request);
					}
					this.mPendingRequests.clear();
				}

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
						if (key.isConnectable()) {
							this.finishConnection(key);
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
	protected void finishConnection(SelectionKey pKey) throws IOException {
		log.debug("finishConnection");
		SocketChannel socketChannel;
		InetSocketAddress address;
		Connection con = (Connection) pKey.attachment();
		if (con != null) {
			socketChannel = con.getSocketChannel();
			address = con.getAddress();
		} else {
			socketChannel = (SocketChannel) pKey.channel();
			address = (InetSocketAddress) socketChannel.socket().getRemoteSocketAddress();
			con = new Connection(address, socketChannel, this.mBufferCapacity);
			pKey.attach(con);
		}

		try {
			socketChannel.finishConnect();
		} catch (NoConnectionPendingException e) {
			log.error("NoConnectionPendingException{}", con.getAddress(), e);
			this.handleConnectionFailure(pKey, socketChannel, address.getAddress());
			return;
		} catch (ClosedChannelException e) {
			log.error("ClosedChannelException{}", con.getAddress(), e);
			this.handleConnectionFailure(pKey, socketChannel, address.getAddress());
			return;
		} catch (IOException e) {
			log.error("IOException: {}", con.getAddress(), e);
			this.handleConnectionFailure(pKey, socketChannel, address.getAddress());
			return;
		}
		synchronized (this.mChannelMap) {
			this.mChannelMap.put(con.mAddress.getAddress(), con);
		}
		pKey.interestOps(SelectionKey.OP_WRITE);
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put(Data.IP, con.getAddress().getAddress().toString());
		map.put(Data.IP_INETADDRESS, con.getAddress());
		this.sendToThread(Flag.CLIENT_CONNECTED.getNumber(), map);
	}

	/**
	 * @throws IOException
	 *             due to {@link SocketChannel#write(ByteBuffer)} call
	 * @throws CancelledKeyException
	 * @see com.niffy.AndEngineLockStepEngine.threads.nio.BaseSelectorThread#write(java.nio.channels.SelectionKey)
	 */
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
		log.debug("Read: {} Cap: {}", address.getAddress(), buffer.capacity());
		buffer.clear();

		// Attempt to read off the channel
		int numRead = 0;
		try {
			numRead = socketChannel.read(buffer);
		} catch (AsynchronousCloseException e) {
			log.error("AsynchronousCloseException", e);
			this.handleConnectionFailure(pKey, socketChannel, address.getAddress());
		} catch (NotYetConnectedException e) {
			log.error("NotYetConnectedException", e);
			this.handleConnectionFailure(pKey, socketChannel, address.getAddress());
		} catch (ClosedChannelException e) {
			log.error("ClosedChannelException", e);
			this.handleConnectionFailure(pKey, socketChannel, address.getAddress());
			synchronized (this.mChannelMap) {
				this.mChannelMap.remove(address.getAddress());
			}
		} catch (IOException e) {
			log.error("IOException", e);
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
	protected void handleChangeRequest(ChangeRequest pChangeRequest) {
		log.debug("Change Request: {}", pChangeRequest.mType);
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
		case ChangeRequest.REGISTER:
			try {
				SelectionKey keyFound = pChangeRequest.mChannel.register(this.mSelector, pChangeRequest.mOps);
				/*
				 * fixes #1
				 * Hack to finish connection. When making the connection the socket 
				 * channel doesn't seem to have the IP address we're connecting to,
				 * which is use to store a connection reference.
				 * So we create one early on!
				 */
				Connection con = null;
				if (pChangeRequest.mSocketAddress != null) {
					con = new Connection(pChangeRequest.mSocketAddress, pChangeRequest.getAsSocketChannel(),
							this.mBufferCapacity);
				} else {
					InetSocketAddress address = new InetSocketAddress(pChangeRequest.mAddress, this.mServerPort);
					con = new Connection(address, pChangeRequest.getAsSocketChannel(), this.mBufferCapacity);
				}
				keyFound.attach(con);
				this.createQueue(con);
			} catch (ClosedChannelException e) {
				log.error("ClosedChannelException", e);
				/* TODO handle this, clean up pending data and pending changes?
				 * And remove from any collections
				 */
			} catch (CancelledKeyException e) {
				log.error("CancelledKeyException", e);
				/* TODO handle this, clean up pending data and pending changes?
				 * And remove from any collections
				 */
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

	@Override
	public void connectTo(InetSocketAddress pAddress) throws IOException {
		this.initiateConnection(pAddress);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================
	protected SocketChannel initiateConnection(final InetSocketAddress pAddress) throws IOException {
		log.debug("InitiateConnection: {}", pAddress);
		synchronized (this.mChannelMap) {
			if (this.mChannelMap.containsKey(pAddress.getAddress())) {
				log.warn("Went to connect to: {} but already in the channel map.", pAddress);
				return null;
			}
		}
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);

		// Kick off connection establishment
		socketChannel.connect(pAddress);

		// Queue a channel registration since the caller is not the
		// selecting thread. As part of the registration we'll register
		// an interest in connection events. These are raised when a channel
		// is ready to complete connection establishment.
		synchronized (this.mPendingChanges) {
			this.mPendingChanges.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT,
					pAddress.getAddress(), pAddress));
		}
		this.mSelector.wakeup();
		return socketChannel;
	}

	@Override
	public void addRequest(LogRequest<IMessage> pMessage) {
		synchronized (this.mPendingRequests) {
			this.mPendingRequests.add(pMessage);
		}
		this.mSelector.wakeup();
	}

	protected void handleLogRequest(LogRequest<IMessage> pRequest) {
		this.mLogManager.addRequest(pRequest);
	}
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
