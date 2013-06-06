package com.niffy.logforwarder.lib.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageDeleteResponse extends Message {
	// ===========================================================
	// Constants
	// ===========================================================
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(MessageDeleteResponse.class);

	// ===========================================================
	// Fields
	// ===========================================================
	protected boolean mDeleted = false;

	// ===========================================================
	// Constructors
	// ===========================================================

	public MessageDeleteResponse() {
		super();
	}

	public MessageDeleteResponse(final int pVersion, final int pFlag) {
		super(pVersion, pFlag);
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void onReadTransmissionData(DataInputStream pDataInputStream) throws IOException {
		this.mDeleted = pDataInputStream.readBoolean();
	}

	@Override
	protected void onWriteTransmissionData(DataOutputStream pDataOutputStream) throws IOException {
		pDataOutputStream.writeBoolean(this.mDeleted);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public void setDeleted(final boolean pDeleted){
		this.mDeleted = pDeleted;
	}
	
	public boolean getDeleted(){
		return this.mDeleted;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
