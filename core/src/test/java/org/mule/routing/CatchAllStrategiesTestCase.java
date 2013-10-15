/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.DefaultMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.RoutingException;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.MessageDispatcher;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.outbound.DefaultOutboundRouterCollection;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.AbstractTransformer;
import org.mule.util.CollectionUtils;

import com.mockobjects.constraint.Constraint;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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
        Mock endpoint = MuleTestUtils.getMockOutboundEndpoint();
        Mock dispatcher = new Mock(MessageDispatcher.class);
        Mock connector = MuleTestUtils.getMockConnector();
        MuleEvent event = getTestEvent("UncaughtEvent");
        strategy.setEndpoint((OutboundEndpoint) endpoint.proxy());

        endpoint.expect("process", C.isA(DefaultMuleEvent.class));

        strategy.process(event);

        endpoint.verify();
        dispatcher.verify();
        connector.verify();

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
        MuleSession session = getTestSession(getTestService(), muleContext);

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
        public Object doTransform(Object src, String encoding) throws TransformerException
        {
            return "Transformed Test Data";
        }
    }

    @Test
    public void testForwardingStrategyWithTransform() throws Exception
    {
        ForwardingCatchAllStrategy strategy = new ForwardingCatchAllStrategy();
        strategy.setSendTransformed(true);
        Mock endpoint = MuleTestUtils.getMockOutboundEndpoint();
        Mock dispatcher = new Mock(MessageDispatcher.class);
        Mock connector = MuleTestUtils.getMockConnector();
        MuleEvent event = getTestEvent("UncaughtEvent");
        strategy.setEndpoint((OutboundEndpoint) endpoint.proxy());

        endpoint.expectAndReturn("getTransformers", CollectionUtils.singletonList(new TestEventTransformer()));
        endpoint.expectAndReturn("getTransformers", CollectionUtils.singletonList(new TestEventTransformer()));
        endpoint.expect("process", new Constraint()
        {
            public boolean eval(Object object)
            {
                if (object instanceof MuleEvent)
                {
                    return "Transformed Test Data".equals(((MuleEvent) object).getMessage().getPayload());
                }
                return false;
            }
        });

        strategy.process(event);

        endpoint.verify();
        dispatcher.verify();
        connector.verify();

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
        filterRouter2.setFilter(new PayloadTypeFilter(StringBuffer.class));
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

        MuleSession session = getTestSession(getTestService(), muleContext);

        messageRouter.process(getTestEvent("hello"));
        assertEquals(1, catchAllCount[0]);
        assertEquals(0, count1[0]);
        assertEquals(0, count2[0]);

        messageRouter.process(getTestEvent(new StringBuffer()));
        assertEquals(1, catchAllCount[0]);
        assertEquals(0, count1[0]);
        assertEquals(1, count2[0]);

        messageRouter.process(getTestEvent(new Exception()));
        assertEquals(1, catchAllCount[0]);
        assertEquals(1, count1[0]);
        assertEquals(1, count2[0]);
    }

}
