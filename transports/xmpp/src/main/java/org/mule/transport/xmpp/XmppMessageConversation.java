/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp;

import org.mule.api.endpoint.ImmutableEndpoint;

import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;

/**
 * {@link XmppConversation} implementation for sending normal Jabber messages.
 */
public class XmppMessageConversation extends AbstractXmppConversation
{
    public XmppMessageConversation(ImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    @Override
    protected PacketFilter createPacketFilter()
    {
        PacketFilter recipientFilter = new FromMatchesFilter(recipient);
        PacketFilter messageTypeFilter = new MessageTypeFilter(Message.Type.normal);
        return new AndFilter(recipientFilter, messageTypeFilter);
    }
    
    public void dispatch(Message message)
    {
        message.setType(Message.Type.normal);
        message.setTo(recipient);
        
        connection.sendPacket(message);
    }
}
