/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint.outbound;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.context.notification.EndpointMessageNotification;
import org.mule.endpoint.AbstractMessageProcessorTestCase;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OutboundNotificationMessageProcessorTestCase extends AbstractMessageProcessorTestCase
{

    @Test
    public void testDispatch() throws Exception
    {
        TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener();
        muleContext.registerListener(listener);

        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null, null, null, 
            MessageExchangePattern.ONE_WAY, null);
        MessageProcessor mp = new OutboundNotificationMessageProcessor(endpoint);
        MuleEvent event = createTestOutboundEvent();
        mp.process(event);

        assertTrue(listener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(EndpointMessageNotification.MESSAGE_DISPATCH_END, listener.messageNotification.getAction());
        assertEquals(endpoint.getEndpointURI().getUri().toString(),
            listener.messageNotification.getEndpoint());
        assertTrue(listener.messageNotification.getSource() instanceof MuleMessage);
        assertEquals(event.getMessage().getPayload(),
            listener.messageNotification.getSource().getPayload());
    }

    @Test
    public void testSend() throws Exception
    {
        TestEndpointMessageNotificationListener listener = new TestEndpointMessageNotificationListener();
        muleContext.registerListener(listener);

        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null, null, null, 
            MessageExchangePattern.REQUEST_RESPONSE, null);
        MessageProcessor mp = new OutboundNotificationMessageProcessor(endpoint);
        MuleEvent event = createTestOutboundEvent();
        mp.process(event);

        assertTrue(listener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(EndpointMessageNotification.MESSAGE_SEND_END, listener.messageNotification.getAction());
        assertEquals(endpoint.getEndpointURI().getUri().toString(),
            listener.messageNotification.getEndpoint());
        assertTrue(listener.messageNotification.getSource() instanceof MuleMessage);
        assertEquals(event.getMessage().getPayload(),
            (listener.messageNotification.getSource()).getPayload());
    }

}
