/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint.outbound;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.context.notification.EndpointMessageNotification;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class OutboundNotificationMessageProcessorTestCase extends AbstractOutboundMessageProcessorTestCase
{

    public void testDispatch() throws Exception
    {
        TestEndpointMessageNotificationListener<EndpointMessageNotification> listener = new TestEndpointMessageNotificationListener<EndpointMessageNotification>();
        muleContext.registerListener(listener);

        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null, null, null, 
            MessageExchangePattern.ONE_WAY, null);
        MessageProcessor mp = new OutboundNotificationMessageProcessor(endpoint);
        MuleEvent event = createTestOutboundEvent(endpoint);
        mp.process(event);

        assertTrue(listener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(EndpointMessageNotification.MESSAGE_DISPATCHED, listener.messageNotification.getAction());
        assertEquals(endpoint.getEndpointURI().getUri().toString(),
            listener.messageNotification.getEndpoint());
        assertTrue(listener.messageNotification.getSource() instanceof MuleMessage);
        assertEquals(event.getMessage().getPayload(),
            ((MuleMessage) listener.messageNotification.getSource()).getPayload());
    }

    public void testSend() throws Exception
    {
        TestEndpointMessageNotificationListener<EndpointMessageNotification> listener = new TestEndpointMessageNotificationListener<EndpointMessageNotification>();
        muleContext.registerListener(listener);

        OutboundEndpoint endpoint = createTestOutboundEndpoint(null, null, null, null, 
            MessageExchangePattern.REQUEST_RESPONSE, null);
        MessageProcessor mp = new OutboundNotificationMessageProcessor(endpoint);
        MuleEvent event = createTestOutboundEvent(endpoint);
        mp.process(event);

        assertTrue(listener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertEquals(EndpointMessageNotification.MESSAGE_SENT, listener.messageNotification.getAction());
        assertEquals(endpoint.getEndpointURI().getUri().toString(),
            listener.messageNotification.getEndpoint());
        assertTrue(listener.messageNotification.getSource() instanceof MuleMessage);
        assertEquals(event.getMessage().getPayload(),
            ((MuleMessage) listener.messageNotification.getSource()).getPayload());
    }

}
