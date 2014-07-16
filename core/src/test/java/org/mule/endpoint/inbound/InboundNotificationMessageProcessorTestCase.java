/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
