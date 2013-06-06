package com.niffy.logforwarder.lib.logmanagement;

import java.net.InetSocketAddress;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.niffy.logforwarder.lib.ISelector;
import com.niffy.logforwarder.lib.messages.IMessage;

public class LogManagerClient extends LogManager<IMessage> {
	// ===========================================================
	// Constants
	// ===========================================================
	private final Logger log = LoggerFactory.getLogger(LogManagerClient.class);

	// ===========================================================
	// Fields
	// ===========================================================
	protected HashMap<Integer, LogRequest<IMessage>> mRequests = new HashMap<Integer, LogRequest<IMessage>>();
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
		IMessage message = this.reproduceMessage(pData, pAddress);
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
		final int pSeq = pMessage.getSequence();
		if (this.mRequests.containsKey(pSeq)) {
			final LogRequest<IMessage> request = this.mRequests.get(pSeq);
			final int pRequestID = request.getClientRequest();
			request.getOwner().handleResponse(pRequestID, pMessage);
			this.mRequests.remove(pSeq);
		} else {
			log.error("Could not serve response to log request owner Seq: {} Flag: {}", pSeq, pMessage.getMessageFlag());
		}
	}
	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
