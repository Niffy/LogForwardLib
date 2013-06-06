package com.niffy.logforwarder.lib.logmanagement;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.niffy.logforwarder.lib.messages.IMessage;

public class LogRequest<T extends IMessage> {
	// ===========================================================
	// Constants
	// ===========================================================
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(LogRequest.class);

	// ===========================================================
	// Fields
	// ===========================================================
	public int mClientRequest;
	public InetSocketAddress mAddress;
	public T mMessage;
	public ILogOwner mOwner;

	// ===========================================================
	// Constructors
	// ===========================================================

	public LogRequest(final int pRequest, final InetSocketAddress pAddress, final T pMessage, final ILogOwner pOwner) {
		this.mClientRequest = pRequest;
		this.mAddress = pAddress;
		this.mMessage = pMessage;
		this.mOwner = pOwner;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================
	public int getClientRequest() {
		return mClientRequest;
	}

	public void setClientRequest(int pClientRequest) {
		this.mClientRequest = pClientRequest;
	}

	public InetSocketAddress getAddress() {
		return mAddress;
	}

	public void setAddress(InetSocketAddress pAddress) {
		this.mAddress = pAddress;
	}

	public T getMessage() {
		return mMessage;
	}

	public void setMessage(T pMessage) {
		this.mMessage = pMessage;
	}

	public ILogOwner getOwner() {
		return this.mOwner;
	}
	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
