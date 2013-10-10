/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
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


