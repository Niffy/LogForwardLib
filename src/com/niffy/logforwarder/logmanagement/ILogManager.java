package com.niffy.logforwarder.logmanagement;

import java.net.InetAddress;

import com.niffy.logforwarder.messages.IMessage;

public interface ILogManager {
	public void handle(final InetAddress pAddress, final byte[] pData);
	public void addRequest(final LogRequest<IMessage> pRequest);
}
