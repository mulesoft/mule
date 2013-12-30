/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.mule.DefaultMuleMessage;
import org.mule.transport.AbstractMuleMessageFactory;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

public class XmppMuleMessageFactory extends AbstractMuleMessageFactory
{

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
    protected void addProperties(DefaultMuleMessage message, Object transportMessage) throws Exception
    {
        super.addProperties(message, transportMessage);
        
        Packet packet = (Packet) transportMessage;

        message.setUniqueId(packet.getPacketID());
        
        Map<String, Object> properties = new HashMap<String, Object>();
        addXmppPacketProperties(packet, properties);

        if (packet instanceof Message)
        {
            Message xmppMessage = (Message) packet;
            addXmppMessageProperties(xmppMessage, properties);
        }
        
        message.addInboundProperties(properties);
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


