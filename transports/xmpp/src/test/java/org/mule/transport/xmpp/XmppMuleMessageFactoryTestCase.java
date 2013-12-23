/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;
import org.mule.util.UUID;

import org.jivesoftware.smack.packet.Message;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class XmppMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new XmppMuleMessageFactory();
    }

    @Override
    protected Object getValidTransportMessage() throws Exception
    {
        Message xmppMessage = new Message();
        xmppMessage.setBody(TEST_MESSAGE);
        return xmppMessage;
    }

    @Override
    protected Object getUnsupportedTransportMessage()
    {
        return "this is an invalid transport message for XmppMuleMessageFactory";
    }
    
    @Test
    public void testPacketWithMessageProperties() throws Exception
    {
        String uuid = UUID.getUUID();
        
        Message payload = (Message) getValidTransportMessage();
        payload.setSubject("the subject");
        payload.setProperty("foo", "foo-value");
        payload.setPacketID(uuid);
     
        MuleMessageFactory factory = createMuleMessageFactory();
        MuleMessage message = factory.create(payload, encoding, muleContext);
        assertNotNull(message);
        assertEquals(Message.class, message.getPayload().getClass());
        assertEquals(TEST_MESSAGE, ((Message) message.getPayload()).getBody());
        
        assertEquals(uuid, message.getUniqueId());
        assertInboundProperty("foo-value", message, "foo");
        assertInboundProperty("the subject", message, XmppConnector.XMPP_SUBJECT);
    }
    
    private void assertInboundProperty(Object expected, MuleMessage message, String key)
    {
        Object value = message.getInboundProperty(key);
        assertEquals(expected, value);
    }
}

