/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import javax.jms.TextMessage;

import org.apache.commons.collections.IteratorUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JmsMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    private static final String MESSAGE_TEXT = "Test JMS Message";

    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new JmsMuleMessageFactory(muleContext);
    }

    @Override
    protected Object getValidTransportMessage() throws Exception
    {
        Mock message = new Mock(TextMessage.class);
        message.expectAndReturn("getText", MESSAGE_TEXT);
        message.expectAndReturn("getJMSCorrelationID", null);
        message.expectAndReturn("getJMSDeliveryMode", Integer.valueOf(1));
        message.expectAndReturn("getJMSDestination", null);
        message.expectAndReturn("getJMSExpiration", Long.valueOf(0));
        message.expectAndReturn("getJMSMessageID", "1234567890");
        message.expectAndReturn("getJMSPriority", Integer.valueOf(4));
        message.expectAndReturn("getJMSRedelivered", Boolean.FALSE);
        message.expectAndReturn("getJMSReplyTo", null);
        message.expectAndReturn("getJMSTimestamp", Long.valueOf(0));
        message.expectAndReturn("getJMSType", null);
        message.expectAndReturn("getPropertyNames", IteratorUtils.asEnumeration(
            IteratorUtils.arrayIterator(new Object[] { "foo" })));
        message.expectAndReturn("getObjectProperty", C.eq("foo"), "bar");
        message.expectAndReturn("equals", C.eq(MESSAGE_TEXT), true);
        
        return message.proxy();
    }

    @Override
    protected Object getUnsupportedTransportMessage()
    {
        return "this is an invalid transport message for JmsMuleMessageFactory";
    }

    @Override
    public void testValidPayload() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();
        
        Object payload = getValidTransportMessage();
        MuleMessage message = factory.create(payload, encoding);
        assertNotNull(message);
        assertEquals(payload, message.getPayload());
        // message factory populates the inbound scope
        assertEquals("bar", message.getInboundProperty("foo"));
    }
}
