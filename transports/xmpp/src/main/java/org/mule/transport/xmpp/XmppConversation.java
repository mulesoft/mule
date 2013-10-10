/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp;

import org.mule.transport.ConnectException;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

/**
 * Implementors of <code>XmppConversation</code> abstract from the XMPP conversation type
 * (e.g. chat, multi user chat or sending of plain jabber messages).
 */
public interface XmppConversation
{
    /**
     * Connect to the Jabber conversation, e.g. join a chat.
     */
    void connect() throws ConnectException;
    
    /**
     * Disconnect from the Jabber conversation, e.g. leave a chat.
     */
    void disconnect();

    /**
     * Asynchronously dispatch <code>message</code> via the Jabber conversation.
     */
    void dispatch(Message message) throws XMPPException;

    /**
     * Wait for a response on this conversation until <code>timeout</code> occurs.
     * 
     * @return {@link Message} next available message or <code>null</code> if timeout occurred.
     */
    Message receive(long timeout);

    /**
     * Wait for a response on this conversation until a message arrives.
     */
    Message receive();
}
