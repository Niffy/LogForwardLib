package com.niffy.logforwarder.lib.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageDeleteRequest extends Message {
	// ===========================================================
	// Constants
	// ===========================================================
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(MessageDeleteRequest.class);

	// ===========================================================
	// Fields
	// ===========================================================
	protected boolean mSDCard = true;
	protected String mLogFileName = "log.txt";
	protected int mLogFileNameSize;
	protected byte[] mLogFileNameData;

	// ===========================================================
	// Constructors
	// ===========================================================

	public MessageDeleteRequest() {
		super();
	}

	public MessageDeleteRequest(final int pVersion, final int pFlag) {
		super(pVersion, pFlag);
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	protected void onReadTransmissionData(DataInputStream pDataInputStream) throws IOException {
		this.mLogFileNameSize = pDataInputStream.readInt();
		this.mLogFileNameData = new byte[this.mLogFileNameSize];
		pDataInputStream.read(this.mLogFileNameData, 0, this.mLogFileNameSize);
		this.mLogFileName = new String(this.mLogFileNameData, "utf-8");
		this.mSDCard = pDataInputStream.readBoolean();
	}

	@Override
	protected void onWriteTransmissionData(DataOutputStream pDataOutputStream) throws IOException {
		pDataOutputStream.writeInt(this.mLogFileNameSize);
		pDataOutputStream.write(this.mLogFileNameData);
		pDataOutputStream.writeBoolean(this.mSDCard);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public void setLogFileNameAndPath(final String pName) {
		this.mLogFileName = pName;
		try {
			this.mLogFileNameData = this.mLogFileName.getBytes("utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		this.mLogFileNameSize = this.mLogFileNameData.length;
	}

	public String getLogFileNameAndPath() {
		return this.mLogFileName;
	}

	public void setSDCard(boolean pSDCard) {
		this.mSDCard = pSDCard;
	}

	public boolean getSDCard() {
		return this.mSDCard;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
