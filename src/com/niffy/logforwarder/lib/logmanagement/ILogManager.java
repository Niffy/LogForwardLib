package com.niffy.logforwarder.lib.logmanagement;

import java.net.InetAddress;

import com.niffy.logforwarder.lib.ISelector;
import com.niffy.logforwarder.lib.messages.IMessage;

public interface ILogManager {
	public void handle(final InetAddress pAddress, final byte[] pData);

	public void addRequest(final LogRequest<IMessage> pRequest);

	public void setSelector(final ISelector pSelector);
}
