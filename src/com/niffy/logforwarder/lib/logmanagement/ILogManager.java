package com.niffy.logforwarder.lib.logmanagement;

import java.net.InetSocketAddress;

import com.niffy.logforwarder.lib.ISelector;
import com.niffy.logforwarder.lib.messages.IMessage;

public interface ILogManager {
	public void handle(final InetSocketAddress pAddress, final byte[] pData);

	public boolean addRequest(final LogRequest<IMessage> pRequest);

	public void setSelector(final ISelector pSelector);
	
	public void newClient(final InetSocketAddress pClient);
	
	public void timeoutClient(final InetSocketAddress pClient);
	
	public void disconnectClient(final InetSocketAddress pClient);
}
