package com.niffy.logforwarder.lib.logmanagement;

import com.niffy.logforwarder.lib.messages.IMessage;

public interface ILogOwner {
	public <T extends IMessage> void handleResponse(final int pRequest, final T Message);
}
