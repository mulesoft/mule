/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint.inbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.endpoint.AbstractMessageProcessorTestCase;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InboundNotificationMessageProcessorTestCase extends AbstractMessageProcessorTestCase
{
    @Test
    public void testProcess() throws Exception
    {
        TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener();
        muleContext.registerListener(listener);

        InboundEndpoint endpoint = createTestInboundEndpoint(null, null);
        MessageProcessor mp = new InboundNotificationMessageProcessor(endpoint);
        MuleEvent event = createTestInboundEvent(endpoint);
        mp.process(event);

        assertTrue(listener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(EndpointMessageNotification.MESSAGE_RECEIVED, listener.messageNotification.getAction());
        assertEquals(endpoint.getEndpointURI().getUri().toString(),
            listener.messageNotification.getEndpoint());
        assertTrue(listener.messageNotification.getSource() instanceof MuleMessage);
        assertEquals(event.getMessage().getPayload(),
            listener.messageNotification.getSource().getPayload());
    }
}
