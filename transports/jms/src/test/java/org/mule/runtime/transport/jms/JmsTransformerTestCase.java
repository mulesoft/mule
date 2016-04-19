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

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.transport.jms.transformers.ObjectToJMSMessage;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.junit.Test;

public class JmsTransformerTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testCustomJMSProperty() throws Exception
    {
        // Warning: this test is REALLY complicated :)
        // The purpose is to test whether custom JMS message properties survive
        // transformations when their name begins with "JMS" (MULE-1120).

        // First we need a JMS message wrapped into a MuleMessage. This turned out to
        // be trickier than expected (ha ha) since mocking a Message depends on the
        // specific calls made to the mocked class.
        TextMessage textMessage = mock(TextMessage.class);
        when(textMessage.getJMSCorrelationID()).thenReturn(null);
        when(textMessage.getJMSMessageID()).thenReturn("1234567890");
        when(textMessage.getJMSDeliveryMode()).thenReturn(Integer.valueOf(1));
        when(textMessage.getJMSDestination()).thenReturn(null);
        when(textMessage.getJMSPriority()).thenReturn(Integer.valueOf(4));
        when(textMessage.getJMSRedelivered()).thenReturn(Boolean.FALSE);
        when(textMessage.getJMSReplyTo()).thenReturn(null);
        when(textMessage.getJMSExpiration()).thenReturn(Long.valueOf(0));
        when(textMessage.getJMSTimestamp()).thenReturn(Long.valueOf(0));
        when(textMessage.getJMSType()).thenReturn(null);
        when(textMessage.getObjectProperty("JMS_CUSTOM_PROPERTY")).thenReturn("customValue");

        MuleMessage msg = new DefaultMuleMessage(textMessage, muleContext);

        // Now we set a custom "JMS-like" property on the MuleMessage
        msg.setOutboundProperty("JMS_CUSTOM_PROPERTY", "customValue");

        // The AbstractJMSTransformer will only apply JMS properties to the
        // underlying message when a "current event" is available, so we need to set
        // one.
        assertNotNull("The test hasn't been configured properly, no muleContext available", muleContext);
        RequestContext.setEvent(new DefaultMuleEvent(msg, MuleTestUtils.getTestEvent("previous", muleContext)));

        // The transformer we are going to use is ObjectToJMSMessage, which will
        // return the same (but mockingly modified!) JMS message that is used as
        // input.
        ObjectToJMSMessage transformer = createObject(ObjectToJMSMessage.class);
        Message transformed = (Message)  transformer.transform(msg.getPayload());

        // Finally we can assert that the setProperty done to the MuleMessage actually
        // made it through to the wrapped JMS Message. Yay!
        assertEquals("customValue", transformed.getObjectProperty("JMS_CUSTOM_PROPERTY"));
    }
}
