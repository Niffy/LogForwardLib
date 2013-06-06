package com.niffy.logforwarder.lib.logmanagement.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallbackTask implements Runnable {
	// ===========================================================
	// Constants
	// ===========================================================
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(CallbackTask.class);

	// ===========================================================
	// Fields
	// ===========================================================
	protected final Runnable task;
	protected final ICallback callback;
	protected final int mSeq;

	// ===========================================================
	// Constructors
	// ===========================================================

	public CallbackTask(final Runnable task, final ICallback callback, final int pSeq) {
		this.task = task;
		this.callback = callback;
		this.mSeq = pSeq;
	}

	@Override
	public void run() {
		task.run();
		callback.complete(this.mSeq);
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
