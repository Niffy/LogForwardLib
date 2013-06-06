package com.niffy.logforwarder.lib.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * (c) 2010 Nicolas Gramlich (c) 2011 Zynga Inc. (c) 2013 Paul Robinson
 * 
 * The contents of a basic message is at least 16.125 bytes (129 bits / 8)
 * 
 * @see IMessage
 * @author Nicolas Gramlich
 * @since 15:27:13 - 18.09.2009
 */
public abstract class Message implements IMessage {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================
	protected int mFlag = -1;
	protected int mVersion = -1;
	protected int mSequence = -1;
	// ===========================================================
	// Constructors
	// ===========================================================
	public Message() {

	}

	public Message(final int pVersion, final int pFlag) {
		this.mFlag = pFlag;
		this.mVersion = pVersion;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	protected abstract void onReadTransmissionData(final DataInputStream pDataInputStream) throws IOException;

	protected abstract void onWriteTransmissionData(final DataOutputStream pDataOutputStream) throws IOException;

	/**
	 * For debugging purposes, append all data of this {@link Message} to the
	 * {@link StringBuilder}.
	 * 
	 * @param pStringBuilder
	 */
	protected void onAppendTransmissionDataForToString(final StringBuilder pStringBuilder) {
		/* Nothing by default. */
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		sb.append(this.getClass().getSimpleName()).append("[getFlag()=").append(this.getMessageFlag());

		this.onAppendTransmissionDataForToString(sb);

		sb.append("]");

		return sb.toString();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}

		final Message other = (Message) obj;

		return this.getMessageFlag() == other.getMessageFlag();
	}

	@Override
	public void setVersion(int pVersion) {
		this.mVersion = pVersion;
	}

	@Override
	public int getVersion() {
		return this.mVersion;
	}

	@Override
	public int getMessageFlag() {
		return this.mFlag;
	}

	@Override
	public void setMessageFlag(int pFlag) {
		this.mFlag = pFlag;
	}
	
	@Override
	public int getSequence() {
		return this.mSequence;
	}

	@Override
	public void setSequence(int pSequence) {
		this.mSequence = pSequence;
	}

	@Override
	public void write(final DataOutputStream pDataOutputStream) throws IOException {
		pDataOutputStream.writeInt(this.mVersion);
		pDataOutputStream.writeInt(this.mFlag);
		pDataOutputStream.writeInt(this.mSequence);
		this.onWriteTransmissionData(pDataOutputStream);
		pDataOutputStream.flush();
	}

	@Override
	public void read(final DataInputStream pDataInputStream) throws IOException {
		this.onReadTransmissionData(pDataInputStream);
	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
