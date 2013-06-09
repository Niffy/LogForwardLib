package com.niffy.logforwarder.lib.logmanagement;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.niffy.logforwarder.lib.Flag;
import com.niffy.logforwarder.lib.GenericClassCastException;
import com.niffy.logforwarder.lib.ISelector;
import com.niffy.logforwarder.lib.messages.IMessage;
import com.niffy.logforwarder.lib.messages.MessageDeleteRequest;
import com.niffy.logforwarder.lib.messages.MessageDeleteResponse;
import com.niffy.logforwarder.lib.messages.MessageError;
import com.niffy.logforwarder.lib.messages.MessageFlag;
import com.niffy.logforwarder.lib.messages.MessageSendRequest;
import com.niffy.logforwarder.lib.messages.MessageSendDataFile;
import com.niffy.logforwarder.lib.messages.MessageSendSizeAck;
import com.niffy.logforwarder.lib.messages.MessageSendSizeResponse;

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
	protected HashMap<InetSocketAddress, ArrayList<IMessage>> mQueue = new HashMap<InetSocketAddress, ArrayList<IMessage>>();

	// ===========================================================
	// Constructors
	// ===========================================================

	public LogManager(final ISelector pSelector, final int pVersion) {
		this.mSelector = pSelector;
		this.mVersion = pVersion;
	}

	public LogManager(final int pVersion) {
		this.mVersion = pVersion;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================
	@Override
	public void handle(InetSocketAddress pAddress, byte[] pData) {

	}

	@Override
	public boolean addRequest(LogRequest<IMessage> pRequest) {
		return false;
	}

	@Override
	public void setSelector(final ISelector pSelector) {
		this.mSelector = pSelector;
	}

	@Override
	public void newClient(InetSocketAddress pClient) {
		this.loopQueueAndSend(pClient);
	}

	@Override
	public void timeoutClient(InetSocketAddress pClient) {

	}

	@Override
	public void disconnectClient(InetSocketAddress pClient) {
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
		} else if (pFlag == MessageFlag.SEND_DATA_FILE.getNumber()) {
			return new MessageSendDataFile(this.mVersion, pFlag);
		} else if (pFlag == MessageFlag.SEND_SIZE_RESPONSE.getNumber()) {
			return new MessageSendSizeResponse(this.mVersion, pFlag);
		} else if (pFlag == MessageFlag.SEND_SIZE_ACK.getNumber()) {
			return new MessageSendSizeAck(this.mVersion, pFlag);
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

	protected IMessage reproduceMessage(byte[] pData, final InetSocketAddress pAddress) {
		log.debug("Reproduce message from: {} size: {}", pAddress, pData.length);
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
				/*
				 *  should the client care about sending a message back to the server?
				 *  this.sendErrorMessage(pAddress, pMessage, sequence);
				 */
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
			log.debug("produced bytes to send: {}", pData.length);
			return pData;
		} catch (IOException e) {
			log.error("Could not stream message into bytes Flag: {} Seq: {}", pMessage.getMessageFlag(),
					pMessage.getSequence());
		}
		return null;
	}

	protected boolean sendMessage(final InetSocketAddress pAddress, final IMessage pMessage) {
		final Object[] pArray = { pMessage.getVersion(), pMessage.getMessageFlag(), pMessage.getSequence(),
				pAddress.toString() };
		log.debug("Sending messsage: V:{} F:{} S:{} to: {}", pArray);
		final byte[] pData = this.produceBytes(pMessage);
		if (pData != null) {
			if (this.mSelector.containsAddress(pAddress.getAddress())) {
				try {
					this.mSelector.send(pAddress, pData);
					return true;
				} catch (IOException e) {
					log.error("Could not send request to: {}", pAddress, e);
					return false;
				}
			} else {
				log.info("Not connected to: {} will connect and queue.", pAddress.getAddress());
				try {
					this.mSelector.connectTo(pAddress);
					this.queueMessage(pAddress, pMessage);
					return true;
				} catch (IOException e) {
					log.error("Could not connect via selector, will not queue message: {}", pAddress, e);
					return false;
				}
			}
		} else {
			log.error("Could not send message flag: {}. As message could not be streamed into bytes",
					pMessage.getMessageFlag());
			return false;
		}
	}

	protected void sendErrorMessage(final InetSocketAddress pAddress, final String pError, final int pSeq) {
		MessageError message = (MessageError) this.getMessage(MessageFlag.ERROR.getNumber());
		if (message != null) {
			message.setSequence(pSeq);
			message.setError(pError);
			this.sendMessage(pAddress, message);
		}
	}

	protected void queueMessage(final InetSocketAddress pAddress, final IMessage pMessage) {
		ArrayList<IMessage> queue = this.mQueue.get(pAddress);
		if (queue == null) {
			queue = new ArrayList<IMessage>();
			this.mQueue.put(pAddress, queue);
		}
		queue.add(pMessage);
	}

	protected void loopQueueAndSend(final InetSocketAddress pAddress) {
		log.debug("Will loop and queue: {}", pAddress.getAddress());
		ArrayList<IMessage> queue = this.mQueue.get(pAddress);
		Iterator<IMessage> it = queue.iterator();
		while (it.hasNext()) {
			IMessage pMessage = it.next();
			it.remove();
			this.sendMessage(pAddress, pMessage);
		}
	}
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
