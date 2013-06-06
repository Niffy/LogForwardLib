package com.niffy.logforwarder.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * (c) 2010 Nicolas Gramlich (c) 2011 Zynga Inc. (c) 2013 Paul Robinson
 * 
 * @author Nicolas Gramlich
 * @since 18:24:50 - 19.09.2009
 */
public interface IMessage {
	// ===========================================================
	// Methods
	// ===========================================================
	/**
	 * Set the version number of network library code.
	 * 
	 * @param pVersion
	 */
	public void setVersion(final int pVersion);

	/**
	 * Get the version number of network library code.
	 * 
	 * @return
	 */
	public int getVersion();

	/**
	 * {@link MessageFlag} of what the message is
	 * 
	 * @return
	 */
	public int getMessageFlag();

	/**
	 * {@link MessageFlag} of what the message is
	 * 
	 * @param pFlag
	 */
	public void setMessageFlag(final int pFlag);

	public int getSequence();

	public void setSequence(final int pSequence);

	/**
	 * Before reading in a message, you should do read it in the follow way. *
	 * <ol>
	 * 1. {@link DataInputStream#readInt()} read in the network library version.
	 * </ol>
	 * <ol>
	 * 2. {@link DataInputStream#readInt()} to get the message flag.
	 * </ol>
	 * <ol>
	 * 3. {@link DataInputStream#readInt()} to get the sequence.
	 * </ol>
	 * Once the steps have been completed you can then call this to read the
	 * message into the correct object.
	 * 
	 * @param pDataInputStream
	 * @throws IOException
	 */
	public void read(final DataInputStream pDataInputStream) throws IOException;

	/**
	 * This will write in the following order *
	 * <ol>
	 * 1. {@link DataOutputStream#writeInt(int)} for the network library version
	 * </ol>
	 * <ol>
	 * 2. {@link DataOutputStream#writeInt(int)} for writing the message flag.
	 * </ol>
	 * <ol>
	 * 3. {@link DataOutputStream#writeInt(int)} for writing the sequence.
	 * </ol>
	 * Writing will then be passed to
	 * {@link #onWriteTransmissionData(DataOutputStream)}
	 * 
	 * @param pDataOutputStream
	 * @throws IOException
	 */
	public void write(final DataOutputStream pDataOutputStream) throws IOException;

}
