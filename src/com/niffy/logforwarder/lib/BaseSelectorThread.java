package com.niffy.logforwarder.lib;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.niffy.logforwarder.lib.logmanagement.ILogManager;

/**
 * Heavily based on James Greefield <a
 * href=""http://rox-xmlrpc.sourceforge.net/niotut/> ROX Java NIO Tutorial</a>
 * 
 * @author Paul Robinson
 * @author <a href="mailto:nio@flat502.com">James Greenfield</a>
 * @since 11 May 2013 15:09:48
 * 
 * @see<a href=""http://rox-xmlrpc.sourceforge.net/niotut/> ROX Java NIO
 *        Tutorial</a>
 */
public abstract class BaseSelectorThread extends Thread implements ISelector {
	// ===========================================================
	// Constants
	// ===========================================================
	private final Logger log = LoggerFactory.getLogger(BaseSelectorThread.class);
	protected static final int DefaultBufferCapacity = 524288;
	// ===========================================================
	// Fields
	// ===========================================================
	protected InetSocketAddress mAddress;
	protected Selector mSelector;
	protected int mBufferCapacity = 8192;
	protected List<ChangeRequest> mPendingChanges = new LinkedList<ChangeRequest>();
	protected Map<InetAddress, ArrayList<ByteBuffer>> mPendingData = new HashMap<InetAddress, ArrayList<ByteBuffer>>();
	protected HashMap<InetAddress, Connection> mChannelMap = new HashMap<InetAddress, Connection>();
	/**
	 * Any {@link InetAddress} in here is pending a closure, so do not add
	 * anymore requests to send.
	 */
	protected ArrayList<InetAddress> mPendingClosure = new ArrayList<InetAddress>();
	protected ILogManager mLogManager;

	// ===========================================================
	// Constructors
	// ===========================================================
	public BaseSelectorThread(final String pName, final InetSocketAddress pAddress, final ILogManager pLogManager)
			throws IOException {
		this(pName, pAddress, DefaultBufferCapacity, pLogManager);
	}

	/**
	 * 
	 * @param pName
	 *            name of thread
	 * @param pAddress
	 *            {@link InetSocketAddress} of client.
	 * @param pCaller
	 *            {@link WeakThreadHandler} to pass messages to.
	 * @param pOptions
	 *            {@link IBaseOptions} to use
	 * @param pBufferCapacity
	 *            What size should the buffer capacity to read and write.
	 * @throws IOException
	 *             when calling {@link #initSelector()}
	 */
	public BaseSelectorThread(final String pName, final InetSocketAddress pAddress, final int pBufferCapacity,
			final ILogManager pLogManager) throws IOException {
		super(pName);
		this.mAddress = pAddress;
		this.mBufferCapacity = pBufferCapacity;
		ByteBuffer.allocate(this.mBufferCapacity);
		this.mSelector = this.initSelector();
	}

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
						log.error("IOException on key operation", e);
					}
				}
			} catch (Exception e) {
				log.error("Exception in main loop", e);
			}
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================
	public void setLogManager(final ILogManager pLogManager) {
		this.mLogManager = pLogManager;
	}

	protected Selector initSelector() throws IOException {
		Selector socketSelector = SelectorProvider.provider().openSelector();
		return socketSelector;
	}

	protected void finishConnection(SelectionKey pKey) throws IOException {

	}

	protected void accept(SelectionKey pKey) throws IOException {
	}

	protected void read(SelectionKey pKey) throws IOException {

	}

	protected void write(SelectionKey pKey) throws IOException, CancelledKeyException {

	}

	protected void handleConnectionFailure(SelectionKey pKey, AbstractSelectableChannel pChannel,
			final InetAddress pAddress) throws IOException {
		log.warn("A connection failure has occured. : {}", pAddress);
		log.warn("Cancel key: {}", pKey.toString());
		pKey.cancel();
		log.warn("Closing channel: {}", pChannel.toString());
		pChannel.close();
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put(Data.IP, pAddress.toString());
		this.sendToThread(Flag.CLIENT_ERROR.getNumber(), map);
	}

	protected void handleConnectionShutdown(SelectionKey pKey, AbstractSelectableChannel pChannel,
			final InetAddress pAddress) throws IOException {
		log.warn("Shuting down connection cleanly: {} ", pAddress);
		pChannel.close();
		pKey.cancel();
		synchronized (this.mChannelMap) {
			if (this.mChannelMap.containsKey(pAddress)) {
				this.mChannelMap.remove(pAddress);
			} else {
				log.error("Went to shut down channel and key cleanly for: {} but not in channel map",
						pAddress.toString());
			}
		}
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put(Data.IP, pAddress.toString());
		this.sendToThread(Flag.CLIENT_DISCONNECT.getNumber(), map);
	}

	protected void handleChangeRequest(final ChangeRequest pChangeRequest) {

	}

	protected void createQueue(final Connection pConnection) {
		synchronized (this.mPendingData) {
			ArrayList<ByteBuffer> queue = this.mPendingData.get(pConnection.getAddress());
			if (queue == null) {
				queue = new ArrayList<ByteBuffer>();
				this.mPendingData.put(pConnection.getAddress().getAddress(), queue);
			}
		}
	}

	@Override
	public void send(InetAddress pAddress, byte[] pData) {
		synchronized (this.mPendingClosure) {
			if (this.mPendingClosure.contains(pAddress)) {
				log.warn("Address: {} is pending closure", pAddress.toString());
			}
		}
		synchronized (this.mChannelMap) {
			if (this.mChannelMap.containsKey(pAddress)) {
				Connection con = this.mChannelMap.get(pAddress);
				if (!con.mSocketChannel.isConnected()) {
					log.error("Went to send a message to: {} but the channel is not connected", pAddress.toString());
				} else {
					this.sendMessage(con, pData);
				}
			} else {
				log.error("Went to send a message to: {} but no channel exists", pAddress.toString());
			}
		}
	}

	protected void sendMessage(final Connection pConnection, final byte[] pData) {
		log.debug("queue message to send to via selector");
		synchronized (this.mPendingData) {
			ArrayList<ByteBuffer> queue = this.mPendingData.get(pConnection.getAddress());
			if (queue == null) {
				queue = new ArrayList<ByteBuffer>();
				this.mPendingData.put(pConnection.getAddress().getAddress(), queue);
			}
			queue.add(ByteBuffer.wrap(pData));
		}
		synchronized (this.mPendingChanges) {
			this.mPendingChanges.add(new ChangeRequest(pConnection.getSocketChannel(), ChangeRequest.CHANGEOPS,
					SelectionKey.OP_WRITE, pConnection.getAddress().getAddress(), pConnection.getAddress()));
		}

		this.mSelector.wakeup();
	}

	protected void sendToThread(final int pFlag, HashMap<String, Object> pMap) {

	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
