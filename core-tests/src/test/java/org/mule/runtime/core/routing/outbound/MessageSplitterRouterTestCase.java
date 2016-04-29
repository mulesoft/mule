/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.routing.outbound.AbstractMessageSplitter;
import org.mule.runtime.core.routing.outbound.SplitMessage;
import org.mule.tck.MuleEventCheckAnswer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.junit.Test;

public class MessageSplitterRouterTestCase extends AbstractMuleContextTestCase
{
    public MessageSplitterRouterTestCase()
    {
        setStartContext(true);
    }

    @Test
    public void testMessageSplitterRouter() throws Exception
    {
        //Async targets
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Endpoint", "test://endpointUri.1");
        OutboundEndpoint mockendpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Endpoint", "test://endpointUri.2");
        OutboundEndpoint mockendpoint2 = RouterTestUtils.createMockEndpoint(endpoint2);

        OutboundEndpoint endpoint3 = getTestOutboundEndpoint("Test3Endpoint", "test://endpointUri.3");
        OutboundEndpoint mockendpoint3 = RouterTestUtils.createMockEndpoint(endpoint3);

        //Sync targets
        OutboundEndpoint endpoint4 = getTestOutboundEndpoint("Test4Endpoint",
            "test://endpointUri.4?exchangePattern=request-response");
        OutboundEndpoint mockendpoint4 = RouterTestUtils.createMockEndpoint(endpoint4);

        OutboundEndpoint endpoint5 = getTestOutboundEndpoint("Test5Endpoint",
            "test://endpointUri.5?exchangePattern=request-response");
        OutboundEndpoint mockendpoint5 = RouterTestUtils.createMockEndpoint(endpoint5);

        OutboundEndpoint endpoint6 = getTestOutboundEndpoint("Test6Endpoint",
            "test://endpointUri.6?exchangePattern=request-response");
        OutboundEndpoint mockendpoint6 = RouterTestUtils.createMockEndpoint(endpoint6);

        // Dummy message splitter
        AbstractMessageSplitter router = new AbstractMessageSplitter()
        {
            @Override
            protected SplitMessage getMessageParts(MuleMessage message, List<MessageProcessor> endpoints)
            {
                int i = 0;
                SplitMessage splitMessage = new SplitMessage();
                for (StringTokenizer tokenizer = new StringTokenizer(message.getPayload().toString(), ","); tokenizer.hasMoreTokens(); i++)
                {
                    String s = tokenizer.nextToken();
                    splitMessage.addPart(s, (OutboundEndpoint) endpoints.get(i));
                }
                return splitMessage;
            }
        };

        router.setMuleContext(muleContext);

        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add(mockendpoint1);
        endpoints.add(mockendpoint2);
        endpoints.add(mockendpoint3);
        router.setRoutes(endpoints);

        MuleMessage message = new DefaultMuleMessage("test,mule,message", muleContext);

        assertTrue(router.isMatch(message));
        when(mockendpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());
        when(mockendpoint2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());
        when(mockendpoint3.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());

        MuleSession session = mock(MuleSession.class);
        router.route(new OutboundRoutingTestEvent(message, session, muleContext));

        endpoints = new ArrayList<MessageProcessor>();
        endpoints.add(mockendpoint4);
        endpoints.add(mockendpoint5);
        endpoints.add(mockendpoint6);
        router.getRoutes().clear();
        router.setRoutes(endpoints);

        message = new DefaultMuleMessage("test,mule,message", muleContext);
        MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);

        when(mockendpoint4.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));
        when(mockendpoint5.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));
        when(mockendpoint6.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));

        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNotNull(result);
        MuleMessage resultMessage = result.getMessage();
        assertNotNull(resultMessage);
        assertTrue(resultMessage.getPayload() instanceof List);
        assertEquals(3, ((List<MuleMessage>) resultMessage.getPayload()).size());
    }
}
