package com.niffy.logforwarder.lib.logmanagement;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PartialDataItem {
	// ===========================================================
	// Constants
	// ===========================================================
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(PartialDataItem.class);

	// ===========================================================
	// Fields
	// ===========================================================
	protected InetSocketAddress mAddress;
	protected ByteBuffer mBuffer;
	protected int mFinalSize;
	protected int mCurrentSize;

	// ===========================================================
	// Constructors
	// ===========================================================

	public PartialDataItem(final InetSocketAddress pAddress, final int pBufferSize) {
		this.mAddress = pAddress;
		this.mBuffer = ByteBuffer.allocate(pBufferSize);
		this.mFinalSize = pBufferSize;
		this.mCurrentSize = 0;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================
	public InetSocketAddress getAddress() {
		return this.mAddress;
	}

	public byte[] getBuffer() {
		return this.mBuffer.array();
	}

	public ByteBuffer getByteBuffer() {
		return this.mBuffer;
	}

	public int getSize() {
		return this.mFinalSize;
	}

	public int getCurrentSize() {
		return this.mCurrentSize;
	}

	// ===========================================================
	// Methods
	// ===========================================================
	public boolean addData(final byte[] pData) {
		this.mBuffer.put(pData, 0, pData.length);
		this.mCurrentSize += pData.length;
		if (this.mCurrentSize == this.mFinalSize) {
			return true;
		}
		return false;
	}
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
