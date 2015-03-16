/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.Message;

/**
 * Implementors of <code>XmppConversation</code> abstract from the XMPP conversation type
 * (e.g. chat, multi user chat or sending of plain jabber messages).
 */
public interface XmppConversation
{
    /**
     * Connect to the Jabber conversation, e.g. join a chat.
     * @throws SmackException 
     * @throws NoResponseException 
     * @throws XMPPException 
     */
    void connect() throws NoResponseException, SmackException, XMPPException;

    /**
     * Disconnect from the Jabber conversation, e.g. leave a chat.
     * @throws NotConnectedException 
     */
    void disconnect() throws NotConnectedException;

    /**
     * Asynchronously dispatch <code>message</code> via the Jabber conversation.
     */
    void dispatch(Message message) throws XMPPException, NotConnectedException;

    /**
     * Adds <code>listener</code> to this conversation's XMPP connection.
     */
    void addPacketListener(PacketListener listener);

    /**
     * Removes <code>listener</code> from this conversation's XMPP connection.
     */
    void removePacketListener(PacketListener listener);

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
