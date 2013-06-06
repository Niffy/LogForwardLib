package com.niffy.logforwarder.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageSendResponse extends Message {
	// ===========================================================
	// Constants
	// ===========================================================
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(MessageSendResponse.class);

	// ===========================================================
	// Fields
	// ===========================================================
	protected int mFileSize = -1;
	protected int mLogFileDataSize;
	protected byte[] mLogFileData;

	// ===========================================================
	// Constructors
	// ===========================================================

	public MessageSendResponse() {
		super();
	}

	public MessageSendResponse(final int pVersion, final int pFlag) {
		super(pVersion, pFlag);
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void onReadTransmissionData(DataInputStream pDataInputStream) throws IOException {
		this.mFileSize = pDataInputStream.readInt();
		this.mLogFileDataSize = pDataInputStream.readInt();
		this.mLogFileData = new byte[this.mLogFileDataSize];
		pDataInputStream.read(this.mLogFileData, 0, this.mLogFileDataSize);
	}

	@Override
	protected void onWriteTransmissionData(DataOutputStream pDataOutputStream) throws IOException {
		pDataOutputStream.writeInt(this.mFileSize);
		pDataOutputStream.writeInt(this.mLogFileDataSize);
		pDataOutputStream.write(this.mLogFileData);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public void setLogFileNameAndPath(final byte[] pData) {
		this.mLogFileData = pData;
		this.mLogFileDataSize = this.mLogFileData.length;
	}

	public byte[] getData() {
		return this.mLogFileData;
	}

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
