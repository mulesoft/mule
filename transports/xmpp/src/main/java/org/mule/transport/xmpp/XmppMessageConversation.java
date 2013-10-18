/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
