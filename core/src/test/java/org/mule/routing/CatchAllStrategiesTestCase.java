/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.RoutingException;
import org.mule.api.transformer.TransformerException;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.outbound.DefaultOutboundRouterCollection;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.AbstractTransformer;
import org.mule.util.CollectionUtils;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class CatchAllStrategiesTestCase extends AbstractMuleContextTestCase
{

    public CatchAllStrategiesTestCase()
    {
        setStartContext(true);
    }

    @Test
    public void testLoggingOnlyStrategy() throws Exception
    {
        // Just test it works without failure
        MuleEvent event = getTestEvent("UncaughtEvent");
        LoggingCatchAllStrategy strategy = new LoggingCatchAllStrategy();
        strategy.process(event);
    }

    @Test
    public void testForwardingStrategy() throws Exception
    {
        ForwardingCatchAllStrategy strategy = new ForwardingCatchAllStrategy();
        OutboundEndpoint endpoint = mock(OutboundEndpoint.class);
        MuleEvent event = getTestEvent("UncaughtEvent");
        strategy.setEndpoint(endpoint);

        strategy.process(event);

        verify(endpoint).process(any(DefaultMuleEvent.class));
        assertNotNull(strategy.getEndpoint());
    }

    /**
     * Test for MULE-3034
     */
    @Test
    public void testForwardingStrategyNullEndpoint() throws Exception
    {
        ForwardingCatchAllStrategy strategy = new ForwardingCatchAllStrategy();
        strategy.setEndpoint(null);
        MuleEvent event = getTestEvent("UncaughtEvent");

        try
        {
            strategy.process(event);
            fail();
        }
        catch (RoutingException sre)
        {
            // we expected this exception
        }
    }

    private static class TestEventTransformer extends AbstractTransformer
    {
        public TestEventTransformer()
        {
            super();
        }

        @Override
        public Object doTransform(Object src, String outputEncoding) throws TransformerException
        {
            return "Transformed Test Data";
        }
    }

    @Test
    public void testForwardingStrategyWithTransform() throws Exception
    {
        OutboundEndpoint endpoint = mock(OutboundEndpoint.class);
        when(endpoint.getTransformers()).thenReturn(CollectionUtils.singletonList(new TestEventTransformer()));
        when(endpoint.process(any(MuleEvent.class))).thenAnswer(new Answer<MuleEvent>()
        {
            @Override
            public MuleEvent answer(InvocationOnMock invocation) throws Throwable
            {
                assertEquals(1, invocation.getArguments().length);
                assertTrue(invocation.getArguments()[0] instanceof MuleEvent);

                MuleEvent event = (MuleEvent) invocation.getArguments()[0];
                assertEquals("Transformed Test Data", event.getMessage().getPayload());

                return null;
            }
        });

        ForwardingCatchAllStrategy strategy = new ForwardingCatchAllStrategy();
        strategy.setSendTransformed(true);
        strategy.setEndpoint(endpoint);

        MuleEvent event = getTestEvent("UncaughtEvent");
        strategy.process(event);

        assertNotNull(strategy.getEndpoint());
    }

    @Test
    public void testFullRouter() throws Exception
    {
        final int[] count1 = new int[]{0};
        final int[] count2 = new int[]{0};
        final int[] catchAllCount = new int[]{0};

        DefaultOutboundRouterCollection messageRouter = new DefaultOutboundRouterCollection();

        FilteringOutboundRouter filterRouter1 = new FilteringOutboundRouter()
        {
            @Override
            public MuleEvent route(MuleEvent event)
            {
                count1[0]++;
                return event;
            }
        };

        FilteringOutboundRouter filterRouter2 = new FilteringOutboundRouter()
        {
            @Override
            public MuleEvent route(MuleEvent event)
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
            public MuleEvent doCatchMessage(MuleEvent event)
            {
                catchAllCount[0]++;
                return null;
            }
        };
        messageRouter.setCatchAllStrategy(strategy);

        messageRouter.process(getTestEvent("hello"));
        assertEquals(1, catchAllCount[0]);
        assertEquals(0, count1[0]);
        assertEquals(0, count2[0]);

        messageRouter.process(getTestEvent(new StringBuilder()));
        assertEquals(1, catchAllCount[0]);
        assertEquals(0, count1[0]);
        assertEquals(1, count2[0]);

        messageRouter.process(getTestEvent(new Exception()));
        assertEquals(1, catchAllCount[0]);
        assertEquals(1, count1[0]);
        assertEquals(1, count2[0]);
    }
}
