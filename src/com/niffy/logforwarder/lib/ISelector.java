package com.niffy.logforwarder.lib;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public interface ISelector {
	/**
	 * This will do nothing on a server.
	 */
	public void connectTo(final InetSocketAddress pAddress) throws IOException;

	public void send(final InetSocketAddress pAddress, final byte[] pData) throws IOException;

	public boolean containsAddress(final InetAddress pAddress);
}
