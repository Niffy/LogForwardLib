package com.niffy.logforwarder.lib;

import java.io.IOException;
import java.net.InetAddress;

public interface ISelector {
	public void send(final InetAddress pAddress, final byte[] pData) throws IOException;
}
