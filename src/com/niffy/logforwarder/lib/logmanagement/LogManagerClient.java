package com.niffy.logforwarder.lib.logmanagement;

import java.net.InetSocketAddress;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.niffy.logforwarder.lib.GenericClassCastException;
import com.niffy.logforwarder.lib.ISelector;
import com.niffy.logforwarder.lib.messages.IMessage;
import com.niffy.logforwarder.lib.messages.MessageFlag;
import com.niffy.logforwarder.lib.messages.MessageSendSizeAck;
import com.niffy.logforwarder.lib.messages.MessageSendSizeResponse;

public class LogManagerClient extends LogManager<IMessage> {
	// ===========================================================
	// Constants
	// ===========================================================
	private final Logger log = LoggerFactory.getLogger(LogManagerClient.class);

	// ===========================================================
	// Fields
	// ===========================================================
	protected HashMap<Integer, LogRequest<IMessage>> mRequests = new HashMap<Integer, LogRequest<IMessage>>();
	protected HashMap<InetSocketAddress, PartialDataItem> mBuffers = new HashMap<InetSocketAddress, PartialDataItem>();

	// ===========================================================
	// Constructors
	// ===========================================================

	public LogManagerClient(final ISelector pSelector, final int pVersion) {
		super(pSelector, pVersion);
	}

	public LogManagerClient(final int pVersion) {
		super(pVersion);
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================
	@Override
	public void handle(InetSocketAddress pAddress, byte[] pData) {
		log.debug("Handle: data size {}", pData.length);
		byte[] data = null;
		/*
		 * TODO we could check data size, if under a certain 
		 * amount then its not a data message.
		 * In future things could get very messy if multiple requests
		 * are served
		 */
		if (this.mBuffers.containsKey(pAddress)) {
			PartialDataItem partialDataItem = this.mBuffers.get(pAddress);
			boolean complete = partialDataItem.addData(pData);
			if (complete) {
				data = partialDataItem.getBuffer();
				this.mBuffers.remove(pAddress);
				log.debug("All data has been collected for message fragment");
			}else{
				return;
			}
		} else {
			data = pData;
		}
		IMessage message = this.reproduceMessage(data, pAddress);
		if (message != null) {
			this.updateOwner(message);
		}
	}

	@Override
	public boolean addRequest(LogRequest<IMessage> pRequest) {
		return this.sendRequest(pRequest);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================
	protected boolean sendRequest(LogRequest<IMessage> pRequest) {
		IMessage message = pRequest.getMessage();
		final int pSeq = this.mSequence.getAndIncrement();
		message.setSequence(pSeq);
		this.mRequests.put(pSeq, pRequest);
		return this.sendMessage(pRequest.getAddress(), message);
	}

	protected void updateOwner(final IMessage pMessage) {
		try {
			MessageFlag flag = MessageFlag.get(pMessage.getMessageFlag());
			log.debug("Update owner for msg: {}:{}", pMessage.getMessageFlag(), flag);
		} catch (GenericClassCastException e) {
			log.debug("Update owner for msg: {}", pMessage.getMessageFlag());
		}

		final int pSeq = pMessage.getSequence();
		if (this.mRequests.containsKey(pSeq)) {
			final LogRequest<IMessage> request = this.mRequests.get(pSeq);
			if (pMessage.getMessageFlag() == MessageFlag.SEND_SIZE_RESPONSE.getNumber()) {
				this.handleSendSizeResponse(request.getAddress(), (MessageSendSizeResponse) pMessage, pSeq);
				return;
			}
			final int pRequestID = request.getClientRequest();
			request.getOwner().handleResponse(pRequestID, pMessage);
			this.mRequests.remove(pSeq);
		} else {
			log.error("Could not serve response to log request owner Seq: {} Flag: {}", pSeq, pMessage.getMessageFlag());
		}
	}

	protected void handleSendSizeResponse(final InetSocketAddress pAddress, final MessageSendSizeResponse pMessage,
			final int pSeq) {
		MessageSendSizeAck ackMessage = (MessageSendSizeAck) this.getMessage(MessageFlag.SEND_SIZE_ACK.getNumber());
		ackMessage.setSequence(pSeq);
		ackMessage.setFileSize(pMessage.getFileSize());
		/*
		 * 20 bytes is added for the Message and MessageSendDataFile overhead
		 * Message - 32 bit - Flag 
		 * Message - 32 bit - Version
		 * Message - 32 bit - Sequence
		 * MessageSendDataFile - FileSize
		 * MessageSendDataFile - FileDataSize
		 */
		final int pSize = pMessage.getFileSize() + 20;
		PartialDataItem item = new PartialDataItem(pAddress, pSize);
		this.mBuffers.put(pAddress, item);
		this.sendMessage(pAddress, ackMessage);
	}
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
