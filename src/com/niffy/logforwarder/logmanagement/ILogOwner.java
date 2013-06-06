package com.niffy.logforwarder.logmanagement;

import com.niffy.logforwarder.messages.IMessage;

public interface ILogOwner {
	public <T extends IMessage> void handleResponse(final int pRequest, final T Message);
}
