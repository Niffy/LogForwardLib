package com.niffy.logforwarder.logmanagement;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.niffy.logforwarder.Flag;
import com.niffy.logforwarder.GenericClassCastException;
import com.niffy.logforwarder.ISelector;
import com.niffy.logforwarder.messages.IMessage;
import com.niffy.logforwarder.messages.MessageDeleteRequest;
import com.niffy.logforwarder.messages.MessageSendResponse;
import com.niffy.logforwarder.messages.MessageError;
import com.niffy.logforwarder.messages.MessageFlag;
import com.niffy.logforwarder.messages.MessageSendRequest;
import com.niffy.logforwarder.messages.MessageDeleteResponse;

public class LogManager<M extends IMessage> implements ILogManager {
	// ===========================================================
	// Constants
	// ===========================================================
	private final Logger log = LoggerFactory.getLogger(LogManager.class);

	// ===========================================================
	// Fields
	// ===========================================================
	protected ISelector mSelector;
	protected AtomicInteger mSequence = new AtomicInteger();
	protected int mVersion = 0;

	// ===========================================================
	// Constructors
	// ===========================================================

	public LogManager(final ISelector pSelector, final int pVersion) {
		this.mSelector = pSelector;
		this.mVersion = pVersion;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================
	@Override
	public void handle(InetAddress pAddress, byte[] pData) {

	}

	@Override
	public void addRequest(LogRequest<IMessage> pRequest) {

	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================
	protected IMessage getMessage(final int pFlag) {
		if (pFlag == MessageFlag.DELETE_REQUEST.getNumber()) {
			return new MessageDeleteRequest(this.mVersion, pFlag);
		} else if (pFlag == MessageFlag.DELETE_RESPONSE.getNumber()) {
			return new MessageDeleteResponse(this.mVersion, pFlag);
		} else if (pFlag == MessageFlag.SEND_REQUEST.getNumber()) {
			return new MessageSendRequest(this.mVersion, pFlag);
		} else if (pFlag == MessageFlag.SEND_RESPONSE.getNumber()) {
			return new MessageSendResponse(this.mVersion, pFlag);
		} else if (pFlag == MessageFlag.ERROR.getNumber()) {
			return new MessageError(this.mVersion, pFlag);
		}
		try {
			Flag flag = Flag.get(pFlag);
			log.error("Wanted message of: {} but no class exists. Called: {}", pFlag, flag);
		} catch (GenericClassCastException e) {

		}
		log.error("Wanted message of: {} but no class exists.", pFlag);
		return null;
	}

	protected IMessage reproduceMessage(byte[] pData, final InetAddress pAddress) {
		try {
			final ByteArrayInputStream bInput = new ByteArrayInputStream(pData);
			DataInputStream dis = new DataInputStream(bInput);
			final int version = dis.readInt();
			final int flag = dis.readInt();
			final int sequence = dis.readInt();
			/* THIS IS STUPID, getting version, flag than sequence while 
			 * version may not match could result in error if the base
			 * message class structure changes*/
			if (version != this.mVersion) {
				final String pMessage = "Version numbers do not match. Ours: " + this.mVersion + " Clients: " + version;
				log.error(pMessage);
				this.sendErrorMessage(pAddress, pMessage, sequence);
				return null;
			}
			IMessage message = this.getMessage(flag);
			if (message != null) {
				message.read(dis);
				message.setSequence(sequence);
				message.setMessageFlag(flag);
				message.setVersion(version);
				return message;
			} else {
				log.error("Could not get message for flag: {}", flag);
			}
		} catch (IOException e) {
			log.error("Could not reconstruct data. Error with input stream. Data: {}", pData);
		}
		return null;
	}

	protected byte[] produceBytes(final IMessage pMessage) {
		try {
			final ByteArrayOutputStream bOutput = new ByteArrayOutputStream();
			final DataOutputStream dOutput = new DataOutputStream(bOutput);
			pMessage.write(dOutput);
			dOutput.flush();
			bOutput.flush();
			byte[] pData = bOutput.toByteArray();
			return pData;
		} catch (IOException e) {
			log.error("Could not stream message into bytes Flag: {} Seq: {}", pMessage.getMessageFlag(),
					pMessage.getSequence());
		}
		return null;
	}

	protected void sendMessage(final InetAddress pAddress, final IMessage pMessage) {
		final byte[] pData = this.produceBytes(pMessage);
		if (pData != null) {
			try {
				this.mSelector.send(pAddress, pData);
			} catch (IOException e) {
				log.error("Could not send request to: {}", pAddress, e);
			}
		} else {
			log.error("Could not send message flag: {}. As message could not be streamed into bytes",
					pMessage.getMessageFlag());
		}
	}

	protected void sendErrorMessage(final InetAddress pAddress, final String pError, final int pSeq) {
		MessageError message = (MessageError) this.getMessage(MessageFlag.ERROR.getNumber());
		if (message != null) {
			message.setSequence(pSeq);
			message.setError(pError);
			this.sendMessage(pAddress, message);
		}
	}
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
