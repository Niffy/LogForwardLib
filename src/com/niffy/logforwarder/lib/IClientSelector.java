package com.niffy.logforwarder.lib;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.niffy.logforwarder.lib.logmanagement.LogRequest;
import com.niffy.logforwarder.lib.messages.IMessage;

public interface IClientSelector extends ISelector {

	/**
	 * Connect to a client-server
	 * 
	 * @param pAddress
	 *            {@link InetSocketAddress} to connect to.
	 * @throws IOException
	 */
	public void connectTo(final InetSocketAddress pAddress) throws IOException;

	public void addRequest(final LogRequest<IMessage> pMessage);
}
