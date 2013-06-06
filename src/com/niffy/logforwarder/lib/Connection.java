package com.niffy.logforwarder.lib;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Connection {
	// ===========================================================
	// Constants
	// ===========================================================
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(Connection.class);

	// ===========================================================
	// Fields
	// ===========================================================
	protected InetSocketAddress mAddress;
	protected SocketChannel mSocketChannel;
	protected ByteBuffer mReadBuffer;
	// ===========================================================
	// Constructors
	// ===========================================================

	public Connection(final InetSocketAddress pAddress, final SocketChannel pSocketChannel, final int pByteBufferSize) {
		this.mAddress = pAddress;
		this.mSocketChannel = pSocketChannel;
		this.mReadBuffer = ByteBuffer.allocate(pByteBufferSize);
	}

	public InetSocketAddress getAddress() {
		return mAddress;
	}

	public void setAddress(InetSocketAddress pAddress) {
		this.mAddress = pAddress;
	}

	public SocketChannel getSocketChannel() {
		return mSocketChannel;
	}

	public void setSocketChannel(SocketChannel pSocketChannel) {
		this.mSocketChannel = pSocketChannel;
	}
	
	public ByteBuffer getBuffer(){
		return this.mReadBuffer;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

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
