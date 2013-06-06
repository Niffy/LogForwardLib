package com.niffy.logforwarder.lib;

import com.niffy.logforwarder.lib.logmanagement.LogRequest;
import com.niffy.logforwarder.lib.messages.IMessage;

public interface IClientSelector extends ISelector {

	public void addRequest(final LogRequest<IMessage> pMessage);
}
