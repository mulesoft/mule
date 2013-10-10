/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
