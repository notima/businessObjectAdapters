package org.notima.businessobjects.adapter.tools;

import org.notima.businessobjects.adapter.tools.exception.MessageSenderException;
import org.notima.generic.businessobjects.Message;
import org.notima.generic.ifacebusinessobjects.KeyManager;

public interface MessageSender {

    public String getType();

    public void send(Message message, KeyManager keyManager, boolean attachSenderPublicKey) throws MessageSenderException;
}
