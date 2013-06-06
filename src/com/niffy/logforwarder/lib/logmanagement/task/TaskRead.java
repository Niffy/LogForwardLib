package com.niffy.logforwarder.lib.logmanagement.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskRead implements Runnable {
	// ===========================================================
	// Constants
	// ===========================================================
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(TaskRead.class);

	// ===========================================================
	// Fields
	// ===========================================================
	protected final String mFile;
	protected byte[] mData;
	protected int mFileSize;

	// ===========================================================
	// Constructors
	// ===========================================================

	public TaskRead(final String pFile) {
		this.mFile = pFile;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================
	@Override
	public void run() {
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================
	public byte[] getData() {
		return this.mData;
	}

	public int getFileSize() {
		return this.mFileSize;
	}
	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
