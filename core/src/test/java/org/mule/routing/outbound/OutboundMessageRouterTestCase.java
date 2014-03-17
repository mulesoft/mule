/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.MatchableMessageProcessor;
import org.mule.api.routing.RoutingException;
import org.mule.routing.AbstractCatchAllStrategy;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.MuleEventCheckAnswer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class OutboundMessageRouterTestCase extends AbstractMuleContextTestCase
{
    public OutboundMessageRouterTestCase()
    {
        setStartContext(true);
    }

    @Test
    public void testOutboundMessageRouter() throws Exception
    {
        DefaultOutboundRouterCollection messageRouter = createObject(DefaultOutboundRouterCollection.class);
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());
        assertNotNull(messageRouter.getCatchAllStrategy());

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider",
            "test://Test1Provider?exchangePattern=one-way");
        assertNotNull(endpoint1);
        OutboundEndpoint mockendpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider");
        assertNotNull(endpoint2);
        OutboundEndpoint mockendpoint2 = RouterTestUtils.createMockEndpoint(endpoint2);

        FilteringOutboundRouter router1 = new FilteringOutboundRouter();
        PayloadTypeFilter filter = new PayloadTypeFilter(String.class);
        router1.setFilter(filter);
        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add(mockendpoint1);
        router1.setRoutes(endpoints);

        FilteringOutboundRouter router2 = new FilteringOutboundRouter();
        PayloadTypeFilter filter2 = new PayloadTypeFilter();
        filter2.setExpectedType(Exception.class);
        router2.setFilter(filter2);
        endpoints = new ArrayList<MessageProcessor>();
        endpoints.add(mockendpoint2);
        router2.setRoutes(endpoints);

        messageRouter.addRoute(router1);
        assertEquals(1, messageRouter.getRoutes().size());
        messageRouter.removeRoute(router1);
        assertEquals(0, messageRouter.getRoutes().size());

        List<MatchableMessageProcessor> list = new ArrayList<MatchableMessageProcessor>();
        list.add(router1);
        list.add(router2);
        messageRouter.setMessageProcessors(list);

        MuleSession session = mock(MuleSession.class);
        MuleEvent event = getTestEvent("test event", session);

        when(mockendpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());
        messageRouter.process(event);

        event = getTestEvent(new IllegalArgumentException(), session);

        when(mockendpoint2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());
        messageRouter.process(event);

        FilteringOutboundRouter router3 = new FilteringOutboundRouter();
        router3.setFilter(new PayloadTypeFilter(Object.class));
        endpoints = new ArrayList<MessageProcessor>();
        endpoints.add(mockendpoint2);
        router3.setRoutes(endpoints);
        messageRouter.addRoute(router3);

        // now the message should be routed twice to different targets
        event = getTestEvent("testing multiple routing", session);

        messageRouter.setMatchAll(true);
        messageRouter.process(event);
    }

    @Test
    public void testRouterWithCatchAll() throws Exception
    {
        final int[] count1 = new int[]{0};
        final int[] count2 = new int[]{0};
        final int[] catchAllCount = new int[]{0};

        DefaultOutboundRouterCollection messageRouter = new DefaultOutboundRouterCollection();

        FilteringOutboundRouter filterRouter1 = new FilteringOutboundRouter()
        {
            @Override
            public MuleEvent route(MuleEvent event)
                throws RoutingException
            {
                count1[0]++;
                return event;
            }
        };

        FilteringOutboundRouter filterRouter2 = new FilteringOutboundRouter()
        {
            @Override
            public MuleEvent route(MuleEvent event)
                throws RoutingException
            {
                count2[0]++;
                return event;
            }
        };

        filterRouter1.setFilter(new PayloadTypeFilter(Exception.class));
        filterRouter2.setFilter(new PayloadTypeFilter(StringBuilder.class));
        messageRouter.addRoute(filterRouter1);
        messageRouter.addRoute(filterRouter2);

        AbstractCatchAllStrategy strategy = new AbstractCatchAllStrategy()
        {
            @Override
            public MuleEvent doCatchMessage(MuleEvent event) throws RoutingException
            {
                catchAllCount[0]++;
                return null;
            }
        };

        messageRouter.setCatchAllStrategy(strategy);

        MuleEvent event = getTestEvent("hello");
        messageRouter.process(event);
        assertEquals(1, catchAllCount[0]);
        assertEquals(0, count1[0]);
        assertEquals(0, count2[0]);

        event = getTestEvent(new StringBuilder());
        messageRouter.process(event);
        assertEquals(1, catchAllCount[0]);
        assertEquals(0, count1[0]);
        assertEquals(1, count2[0]);

        event = getTestEvent(new Exception());
        messageRouter.process(event);
        assertEquals(1, catchAllCount[0]);
        assertEquals(1, count1[0]);
        assertEquals(1, count2[0]);
    }

    @Test
    public void testCorrelation() throws Exception
    {
        FilteringOutboundRouter filterRouter = new FilteringOutboundRouter();
        MuleMessage message = new DefaultMuleMessage(new StringBuilder(), muleContext);
        OutboundEndpoint endpoint = getTestOutboundEndpoint("test");
        filterRouter.setMessageProperties(getTestService(), message, endpoint);
        assertNotNull(message.getCorrelationId());
    }
}
