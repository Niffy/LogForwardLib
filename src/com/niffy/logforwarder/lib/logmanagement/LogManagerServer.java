package com.niffy.logforwarder.lib.logmanagement;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.niffy.logforwarder.lib.ISelector;
import com.niffy.logforwarder.lib.logmanagement.task.CallbackInfo;
import com.niffy.logforwarder.lib.logmanagement.task.CallbackTask;
import com.niffy.logforwarder.lib.logmanagement.task.ICallback;
import com.niffy.logforwarder.lib.logmanagement.task.TaskDelete;
import com.niffy.logforwarder.lib.logmanagement.task.TaskRead;
import com.niffy.logforwarder.lib.messages.IMessage;
import com.niffy.logforwarder.lib.messages.MessageDeleteRequest;
import com.niffy.logforwarder.lib.messages.MessageDeleteResponse;
import com.niffy.logforwarder.lib.messages.MessageFlag;
import com.niffy.logforwarder.lib.messages.MessageSendRequest;
import com.niffy.logforwarder.lib.messages.MessageSendResponse;

public class LogManagerServer extends LogManager<IMessage> implements ICallback {
	// ===========================================================
	// Constants
	// ===========================================================
	private final Logger log = LoggerFactory.getLogger(LogManagerServer.class);

	// ===========================================================
	// Fields
	// ===========================================================
	/**
	 * Key = Our Seq. Value = {@link CallbackInfo}
	 */
	protected HashMap<Integer, CallbackInfo> mRunnables = new HashMap<Integer, CallbackInfo>();
	protected ExecutorService mService;

	// ===========================================================
	// Constructors
	// ===========================================================

	public LogManagerServer(final ISelector pSelector, final int pVersion) {
		super(pSelector, pVersion);
		this.mService = Executors.newSingleThreadExecutor();
	}

	public LogManagerServer(final int pVersion) {
		super(pVersion);
		this.mService = Executors.newSingleThreadExecutor();
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================
	@Override
	public void complete(int pSeq) {
		CallbackInfo info = this.mRunnables.get(pSeq);
		if (info != null) {
			this.createResponse(info);
		} else {
			log.error("Could not find callback info for seq: {}", pSeq);
		}
	}

	@Override
	public void handle(InetSocketAddress pAddress, byte[] pData) {
		IMessage message = this.reproduceMessage(pData, pAddress);
		if (message != null) {
			this.produceRequestResponse(pAddress, message);
		} else {
			log.error("Could not reproduce message");
		}
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods
	// ===========================================================
	protected void produceRequestResponse(final InetSocketAddress pAddress, final IMessage pMessage) {
		if (pMessage.getMessageFlag() == MessageFlag.DELETE_REQUEST.getNumber()) {
			this.createDeleteTask(pAddress, (MessageDeleteRequest) pMessage);
		} else if (pMessage.getMessageFlag() == MessageFlag.SEND_REQUEST.getNumber()) {
			this.createReadTask(pAddress, (MessageSendRequest) pMessage);
		} else {
			log.error("Unknown task. Message Flag : {}", pMessage.getMessageFlag());
		}
	}

	/**
	 * Android service should override this to produce its own task
	 * 
	 * @param pAddress
	 * @param pMessage
	 */
	protected void createReadTask(final InetSocketAddress pAddress, final MessageSendRequest pMessage) {
		final int pSeq = this.mSequence.getAndIncrement();
		Runnable runnable = new TaskRead(pMessage.getLogFileNameAndPath());
		CallbackInfo info = new CallbackInfo(pAddress, pSeq, pMessage.getSequence(), pMessage.getMessageFlag(),
				runnable);
		this.mRunnables.put(pSeq, info);
		CallbackTask task = new CallbackTask(runnable, this, pSeq);
		this.mService.execute(task);
	}

	/**
	 * Android service should override this to produce its own task
	 * 
	 * @param pAddress
	 * @param pMessage
	 */
	protected void createDeleteTask(final InetSocketAddress pAddress, final MessageDeleteRequest pMessage) {
		final int pSeq = this.mSequence.getAndIncrement();
		Runnable runnable = new TaskDelete(pMessage.getLogFileNameAndPath());
		CallbackInfo info = new CallbackInfo(pAddress, pSeq, pMessage.getSequence(), pMessage.getMessageFlag(),
				runnable);
		this.mRunnables.put(pSeq, info);
		CallbackTask task = new CallbackTask(runnable, this, pSeq);
		this.mService.execute(task);
	}

	protected void createResponse(final CallbackInfo pCallbackInfo) {
		if (pCallbackInfo.getMessageFlag() == MessageFlag.SEND_REQUEST.getNumber()) {
			this.createReadResponse(pCallbackInfo);
		} else if (pCallbackInfo.getMessageFlag() == MessageFlag.DELETE_REQUEST.getNumber()) {
			this.createDeleteResponse(pCallbackInfo);
		} else {
			log.error("Unknown task. Message Flag : {}", pCallbackInfo.getMessageFlag());
		}
	}

	/**
	 * Android service should override this to produce message from its own task
	 * 
	 * @param pAddress
	 * @param pMessage
	 */
	protected void createReadResponse(final CallbackInfo pCallbackInfo) {
		MessageSendResponse pMessage = (MessageSendResponse) this.getMessage(MessageFlag.SEND_RESPONSE.getNumber());
		pMessage.setSequence(pCallbackInfo.getClientSeq());
		TaskRead runnable = (TaskRead) pCallbackInfo.getRunnable();
		pMessage.setFileSize(runnable.getFileSize());
		pMessage.setLogFileNameAndPath(runnable.getData());
		this.sendMessage(pCallbackInfo.getAddress(), pMessage);
	}

	/**
	 * Android service should override this to produce message from its own task
	 * 
	 * @param pAddress
	 * @param pMessage
	 */
	protected void createDeleteResponse(final CallbackInfo pCallbackInfo) {
		MessageDeleteResponse pMessage = (MessageDeleteResponse) this.getMessage(MessageFlag.DELETE_RESPONSE
				.getNumber());
		pMessage.setSequence(pCallbackInfo.getClientSeq());
		TaskDelete runnable = (TaskDelete) pCallbackInfo.getRunnable();
		pMessage.setDeleted(runnable.getTaskSuccesful());
		this.sendMessage(pCallbackInfo.getAddress(), pMessage);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

}
