package com.niffy.logforwarder.lib.logmanagement.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskDelete implements Runnable {
	// ===========================================================
	// Constants
	// ===========================================================
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(TaskDelete.class);

	// ===========================================================
	// Fields
	// ===========================================================
	protected final String mFile;
	protected boolean mTaskSuccessful = false;

	// ===========================================================
	// Constructors
	// ===========================================================

	public TaskDelete(final String pFile) {
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
	public boolean getTaskSuccesful() {
		return this.mTaskSuccessful;
	}
	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
