package com.niffy.logforwarder.lib.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageSendSizeResponse extends Message {
	// ===========================================================
	// Constants
	// ===========================================================
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(MessageSendSizeResponse.class);

	// ===========================================================
	// Fields
	// ===========================================================
	protected int mFileSize = -1;
	// ===========================================================
	// Constructors
	// ===========================================================

	public MessageSendSizeResponse() {
		super();
	}

	public MessageSendSizeResponse(final int pVersion, final int pFlag) {
		super(pVersion, pFlag);
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void onReadTransmissionData(DataInputStream pDataInputStream) throws IOException {
		this.mFileSize = pDataInputStream.readInt();
	}

	@Override
	protected void onWriteTransmissionData(DataOutputStream pDataOutputStream) throws IOException {
		pDataOutputStream.writeInt(this.mFileSize);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public void setFileSize(final int pSize) {
		this.mFileSize = pSize;
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
