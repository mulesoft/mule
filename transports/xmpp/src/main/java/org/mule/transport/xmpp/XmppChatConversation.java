/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.mule.api.endpoint.ImmutableEndpoint;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;

/**
 * {@link XmppConversation} implementation that sends messages via {@link Chat}
 */
public class XmppChatConversation extends AbstractXmppConversation
{
    private Chat chat;

    public XmppChatConversation(ImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    @Override
    protected void doConnect()
    {
        chat = connection.getChatManager().createChat(recipient, null);
    }

    @Override
    protected PacketFilter createPacketFilter()
    {
        // The smack API provides Chat.createCollector to create a PacketCollector for a given chat.
        // We cannot reasonably use this, however because smack uses a ThreadFilter internally
        // to match the chat's thread ID. While testing with some Jabber clients (Psi, Spark)
        // it became obvious that the thread ID is not always preserved. Filtering for a given
        // thread id would then prevent the PacketCollector to see incoming chat messages.
        // We create our own PacketFilter here which matches only our chat partner's JID and
        // the message type, just in case.
        PacketFilter recipientFilter = new FromMatchesFilter(recipient);
        PacketFilter messageTypeFilter = new MessageTypeFilter(Message.Type.chat);
        return new AndFilter(recipientFilter, messageTypeFilter);
    }

    @Override
    protected void doDisconnect()
    {
        chat = null;
    }

    @Override
    public void dispatch(Message message) throws XMPPException
    {
        message.setType(Message.Type.chat);
        chat.sendMessage(message);
    }
}
