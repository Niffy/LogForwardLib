package com.niffy.logforwarder.logmanagement.task;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallbackInfo {
	// ===========================================================
	// Constants
	// ===========================================================
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(CallbackInfo.class);

	// ===========================================================
	// Fields
	// ===========================================================
	protected final int mSeq;
	protected final int mClientSeq;
	protected final int mMessageFlag;
	protected final Runnable mRunnable;
	protected final InetAddress mAddress;

	// ===========================================================
	// Constructors
	// ===========================================================

	public CallbackInfo(final InetAddress pAddress, final int pSeq, final int pClientSeq, final int pMessageFlag,
			final Runnable pRunnable) {
		this.mAddress = pAddress;
		this.mSeq = pSeq;
		this.mClientSeq = pClientSeq;
		this.mMessageFlag = pMessageFlag;
		this.mRunnable = pRunnable;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================
	public int getClientSeq() {
		return mClientSeq;
	}

	public int getMessageFlag() {
		return mMessageFlag;
	}

	public Runnable getRunnable() {
		return mRunnable;
	}

	public int getSeq() {
		return mSeq;
	}

	public InetAddress getAddress() {
		return this.mAddress;
	}
	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
