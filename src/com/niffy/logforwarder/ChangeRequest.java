package com.niffy.logforwarder;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

public class ChangeRequest {
	// ===========================================================
	// Constants
	// ===========================================================
	public static final int REGISTER = 1;
	public static final int CHANGEOPS = 2;
	public static final int REMOVECLIENT = 3;
	// ===========================================================
	// Fields
	// ===========================================================
	public AbstractSelectableChannel mChannel;
	public int mType;
	public int mOps;
	public InetAddress mAddress;
	/**
	 * This can be null
	 */
	public InetSocketAddress mSocketAddress;

	// ===========================================================
	// Constructors
	// ===========================================================
	/**
	 * 
	 * @param pSocketChannel
	 * @param pType
	 * @param pOps
	 * @param pAddress
	 * @param pSocketAddress Can be <code>null</code>
	 */
	public ChangeRequest(AbstractSelectableChannel pSocketChannel, int pType, int pOps, InetAddress pAddress,
			InetSocketAddress pSocketAddress) {
		this.mChannel = pSocketChannel;
		this.mType = pType;
		this.mOps = pOps;
		this.mAddress = pAddress;
		this.mSocketAddress = pSocketAddress;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================
	/**
	 * 
	 * @return {@link AbstractSelectableChannel} casted as {@link SocketChannel}
	 */
	public SocketChannel getAsSocketChannel() {
		return (SocketChannel) this.mChannel;
	}
	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
