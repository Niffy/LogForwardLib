package com.niffy.logforwarder.lib.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageShutDownService extends Message {
	// ===========================================================
	// Constants
	// ===========================================================
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(MessageShutDownService.class);

	// ===========================================================
	// Fields
	// ===========================================================

	// ===========================================================
	// Constructors
	// ===========================================================

	public MessageShutDownService() {
		super();
	}

	public MessageShutDownService(final int pVersion, final int pFlag) {
		super(pVersion, pFlag);
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void onReadTransmissionData(DataInputStream pDataInputStream) throws IOException {
	}

	@Override
	protected void onWriteTransmissionData(DataOutputStream pDataOutputStream) throws IOException {
	}

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
