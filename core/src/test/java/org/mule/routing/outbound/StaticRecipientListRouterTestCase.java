/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.MuleEventCheckAnswer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class StaticRecipientListRouterTestCase extends AbstractMuleContextTestCase
{
    public StaticRecipientListRouterTestCase()
    {
        setStartContext(true);
    }

    @Test
    public void testRecipientListRouterAsync() throws Exception
    {
        MuleSession session = mock(MuleSession.class);

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider");
        assertNotNull(endpoint1);
        OutboundEndpoint mockendpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

        List<String> recipients = new ArrayList<String>();
        String recipient1 = "test://recipient1";
        recipients.add(recipient1);
        String recipient2 = "test://recipient2";
        recipients.add(recipient2);
        MockingStaticRecipientList router = createObject(MockingStaticRecipientList.class);

        router.setRecipients(recipients);

        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add(mockendpoint1);
        router.setRoutes(endpoints);
        router.setMuleContext(muleContext);

        assertEquals(2, router.getRecipients().size());

        MuleMessage message = new DefaultMuleMessage("test event", muleContext);
        assertTrue(router.isMatch(message));

        // Set up the mock targets as we discover them
        router.setMockEndpointListener(new MockEndpointListener()
        {
            @Override
            public void mockEndpointAdded(OutboundEndpoint recipient) throws MuleException
            {
                when(recipient.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());
            }
        });

        router.route(new OutboundRoutingTestEvent(message, session, muleContext));
    }


    @Test
    public void testRecipientListRouterSync() throws Exception
    {
        MuleSession session = mock(MuleSession.class);

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider");
        assertNotNull(endpoint1);

        List<String> recipients = new ArrayList<String>();
        recipients.add("test://recipient1?exchangePattern=request-response");
        recipients.add("test://recipient2?exchangePattern=request-response");
        MockingStaticRecipientList router = createObject(MockingStaticRecipientList.class);

        router.setRecipients(recipients);

        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add(endpoint1);
        router.setRoutes(endpoints);
        router.setMuleContext(muleContext);

        assertEquals(2, router.getRecipients().size());

        MuleMessage message = new DefaultMuleMessage("test event", muleContext);
        assertTrue(router.isMatch(message));
        // note this router clones targets so that the endpointUri can be
        // changed

        // The static recipient list router duplicates the message for each endpoint
        // so we can't
        // check for equality on the arguments passed to the dispatch / send methods
        message = new DefaultMuleMessage("test event", muleContext);
        final MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);

        // Set up the mock targets as we discover them
        router.setMockEndpointListener(new MockEndpointListener()
        {
            @Override
            public void mockEndpointAdded(OutboundEndpoint recipient) throws MuleException
            {
                when(recipient.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));
            }
        });

        router.getRecipients().add("test://recipient3?exchangePattern=request-response");
        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNotNull(result);
        MuleMessage resultMessage = result.getMessage();
        assertNotNull(resultMessage);
        assertTrue(resultMessage.getPayload() instanceof List);
        assertEquals(3, ((List<?>) resultMessage.getPayload()).size());
    }

    @Test
    public void testBadRecipientListRouter() throws Exception
    {
        MuleSession session = mock(MuleSession.class);

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider");
        assertNotNull(endpoint1);

        List<String> recipients = new ArrayList<String>();
        recipients.add("malformed-endpointUri-recipient1");
        StaticRecipientList router = createObject(StaticRecipientList.class);

        router.setRecipients(recipients);

        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add(endpoint1);
        router.setRoutes(endpoints);

        assertEquals(1, router.getRecipients().size());

        MuleMessage message = new DefaultMuleMessage("test event", muleContext);
        assertTrue(router.isMatch(message));
        try
        {
            router.route(new OutboundRoutingTestEvent(message, session, muleContext));
            fail("Should not allow malformed endpointUri");
        }
        catch (Exception e)
        {
            // ignore
        }
    }

    /** subclass the router, so that we can mock the targets it creates dynamically.  */
    public static class MockingStaticRecipientList extends StaticRecipientList
    {
        private Map<String, OutboundEndpoint> recipients = new HashMap<String, OutboundEndpoint>();
        private MockEndpointListener listener;

        OutboundEndpoint getRecipient(String name)
        {
            return recipients.get(name);
        }

        public void setMockEndpointListener(MockEndpointListener listener)
        {
            this.listener = listener;
        }

        @Override
        protected OutboundEndpoint getRecipientEndpointFromString(MuleMessage message, String recipient) throws MuleException
        {
            OutboundEndpoint endpoint = super.getRecipientEndpointFromString(message, recipient);
            if (!recipients.containsKey(recipient))
            {
                OutboundEndpoint mock = RouterTestUtils.createMockEndpoint(endpoint);
                recipients.put(recipient, mock);
                if (listener != null)
                {
                    listener.mockEndpointAdded(mock);
                }
            }
            return recipients.get(recipient);
        }
    }

    /** Callback called when new recipient is added */
    interface MockEndpointListener
    {
        void mockEndpointAdded(OutboundEndpoint recipient) throws MuleException;
    }
}
