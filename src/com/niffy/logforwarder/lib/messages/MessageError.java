package com.niffy.logforwarder.lib.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageError extends Message {
	// ===========================================================
	// Constants
	// ===========================================================
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(MessageError.class);

	// ===========================================================
	// Fields
	// ===========================================================
	protected String mError = "log.txt";
	protected int mErrorSize;
	protected byte[] mErrorData;

	// ===========================================================
	// Constructors
	// ===========================================================

	public MessageError() {
		super();
	}

	public MessageError(final int pVersion, final int pFlag) {
		super(pVersion, pFlag);
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void onReadTransmissionData(DataInputStream pDataInputStream) throws IOException {
		this.mErrorSize = pDataInputStream.readInt();
		this.mErrorData = new byte[this.mErrorSize];
		pDataInputStream.read(this.mErrorData, 0, this.mErrorSize);
		this.mError = new String(this.mErrorData, "utf-8");
	}

	@Override
	protected void onWriteTransmissionData(DataOutputStream pDataOutputStream) throws IOException {
		pDataOutputStream.writeInt(this.mErrorSize);
		pDataOutputStream.write(this.mErrorData);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public void setError(final String pError) {
		this.mError = pError;
		try {
			this.mErrorData = this.mError.getBytes("utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		this.mErrorSize = this.mErrorData.length;
	}

	public String getError() {
		return this.mError;
	}


	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
