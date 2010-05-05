/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.xmpp;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.transport.AbstractMuleMessageFactory;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

public class XmppMuleMessageFactory extends AbstractMuleMessageFactory
{
    public XmppMuleMessageFactory(MuleContext context)
    {
        super(context);
    }

    @Override
    protected Class<?>[] getSupportedTransportMessageTypes()
    {
        return new Class[] { Packet.class };
    }

    @Override
    protected Object extractPayload(Object transportMessage, String encoding) throws Exception
    {
        return transportMessage;
    }

    @Override
    protected void addProperties(MuleMessage message, Object transportMessage) throws Exception
    {
        Packet packet = (Packet) transportMessage;

        ((DefaultMuleMessage) message).setUniqueId(packet.getPacketID());
        
        Map<String, Object> properties = new HashMap<String, Object>();
        addXmppPacketProperties(packet, properties);

        if (packet instanceof Message)
        {
            Message xmppMessage = (Message) packet;
            addXmppMessageProperties(xmppMessage, properties);
        }
        
        ((DefaultMuleMessage) message).addInboundProperties(properties);
    }

    private void addXmppPacketProperties(Packet packet, Map<String, Object> properties)
    {
        for (String key : packet.getPropertyNames())
        {
            properties.put(key, packet.getProperty(key));
        }        
    }

    private void addXmppMessageProperties(Message message, Map<String, Object> properties)
    {
        properties.put(XmppConnector.XMPP_SUBJECT, message.getSubject());
        properties.put(XmppConnector.XMPP_THREAD, message.getThread());
    }
}


