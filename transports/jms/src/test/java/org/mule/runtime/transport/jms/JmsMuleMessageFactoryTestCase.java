/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transport.MuleMessageFactory;
import org.mule.runtime.core.transport.AbstractMuleMessageFactoryTestCase;

import javax.jms.TextMessage;

import org.apache.commons.collections.IteratorUtils;

public class JmsMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    private static final String MESSAGE_TEXT = "Test JMS Message";

    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new JmsMuleMessageFactory();
    }

    @Override
    protected Object getValidTransportMessage() throws Exception
    {
        TextMessage textMessage = mock(TextMessage.class);
        when(textMessage.getText()).thenReturn(MESSAGE_TEXT);
        when(textMessage.getJMSCorrelationID()).thenReturn(null);
        when(textMessage.getJMSDeliveryMode()).thenReturn(Integer.valueOf(1));
        when(textMessage.getJMSDestination()).thenReturn(null);
        when(textMessage.getJMSExpiration()).thenReturn(Long.valueOf(0));
        when(textMessage.getJMSMessageID()).thenReturn("1234567890");
        when(textMessage.getJMSPriority()).thenReturn(Integer.valueOf(4));
        when(textMessage.getJMSRedelivered()).thenReturn(Boolean.FALSE);
        when(textMessage.getJMSReplyTo()).thenReturn(null);
        when(textMessage.getJMSTimestamp()).thenReturn(Long.valueOf(0));
        when(textMessage.getJMSType()).thenReturn(null);
        when(textMessage.getPropertyNames()).thenReturn(
            IteratorUtils.asEnumeration(IteratorUtils.arrayIterator(new Object[] { "foo" })));
        when(textMessage.getObjectProperty("foo")).thenReturn("bar");
        return textMessage;
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
        MuleMessage message = factory.create(payload, encoding, muleContext);
        assertNotNull(message);
        assertEquals(payload, message.getPayload());
        // message factory populates the inbound scope
        assertEquals("bar", message.getInboundProperty("foo"));
    }
}
