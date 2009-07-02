/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.ServiceRoutingException;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.MessageDispatcher;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.outbound.DefaultOutboundRouterCollection;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.transformer.AbstractTransformer;
import org.mule.util.CollectionUtils;

import com.mockobjects.constraint.Constraint;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.HashMap;

public class CatchAllStrategiesTestCase extends AbstractMuleTestCase
{

    public void testLoggingOnlyStrategy() throws Exception
    {
        //Just test it works without failure
        MuleEvent event = getTestEvent("UncaughtEvent");
        LoggingCatchAllStrategy strategy = new LoggingCatchAllStrategy();
        strategy.catchMessage(event.getMessage(), null);
    }

    public void testForwardingStrategy() throws Exception
    {
        ForwardingCatchAllStrategy strategy = new ForwardingCatchAllStrategy();
        Mock endpoint = MuleTestUtils.getMockOutboundEndpoint();
        Mock dispatcher = new Mock(MessageDispatcher.class);
        Mock connector = MuleTestUtils.getMockConnector();
        MuleEvent event = getTestEvent("UncaughtEvent");
        strategy.setEndpoint((OutboundEndpoint)endpoint.proxy());

        endpoint.expectAndReturn("isSynchronous", false);
        endpoint.expectAndReturn("isSynchronous", false);
        endpoint.expectAndReturn("getProperties", new HashMap());
        endpoint.expectAndReturn("getProperties", new HashMap());
        endpoint.expectAndReturn("getEndpointURI", new MuleEndpointURI("test://dummy", muleContext));
        endpoint.expectAndReturn("getEndpointURI", new MuleEndpointURI("test://dummy", muleContext));
        endpoint.expect("dispatch", C.isA(DefaultMuleEvent.class));

        strategy.catchMessage(event.getMessage(), null);

        endpoint.verify();
        dispatcher.verify();
        connector.verify();

        assertNotNull(strategy.getEndpoint());
    }

    /**
     * Test for MULE-3034
     */
    public void testForwardingStrategyNullEndpoint() throws Exception
    {
        ForwardingCatchAllStrategy strategy = new ForwardingCatchAllStrategy();
        strategy.setEndpoint(null);
        MuleEvent event = getTestEvent("UncaughtEvent");
        MuleSession session = getTestSession(getTestService(), muleContext);
     
        try
        {
            strategy.catchMessage(event.getMessage(), session);
            fail();
        }
        catch (ServiceRoutingException sre)
        {
            // we expected this exception
        }
    }
    
    private class TestEventTransformer extends AbstractTransformer
    {
        public Object doTransform(Object src, String encoding) throws TransformerException
        {
            return "Transformed Test Data";
        }
    }

    public void testForwardingStrategyWithTransform() throws Exception
    {
        ForwardingCatchAllStrategy strategy = new ForwardingCatchAllStrategy();
        strategy.setSendTransformed(true);
        Mock endpoint = MuleTestUtils.getMockOutboundEndpoint();
        Mock dispatcher = new Mock(MessageDispatcher.class);
        Mock connector = MuleTestUtils.getMockConnector();
        MuleEvent event = getTestEvent("UncaughtEvent");
        strategy.setEndpoint((OutboundEndpoint) endpoint.proxy());

        endpoint.expectAndReturn("isSynchronous", true);
        endpoint.expectAndReturn("isSynchronous", true);

        endpoint.expectAndReturn("getTransformers", CollectionUtils.singletonList(new TestEventTransformer()));
        endpoint.expectAndReturn("getTransformers", CollectionUtils.singletonList(new TestEventTransformer()));
        endpoint.expectAndReturn("getProperties", new HashMap());
        endpoint.expectAndReturn("getProperties", new HashMap());
        endpoint.expectAndReturn("getEndpointURI", new MuleEndpointURI("test://dummy", muleContext));
        endpoint.expectAndReturn("getEndpointURI", new MuleEndpointURI("test://dummy", muleContext));
        endpoint.expect("send", new Constraint()
        {
            public boolean eval(Object arg0)
            {
                if (arg0 instanceof MuleEvent)
                {
                    return "Transformed Test Data".equals(((MuleEvent)arg0).getMessage().getPayload());
                }
                return false;
            }
        });

        strategy.catchMessage(event.getMessage(), null);

        endpoint.verify();
        dispatcher.verify();
        connector.verify();

        assertNotNull(strategy.getEndpoint());
    }

    public void testFullRouter() throws Exception
    {
        final int[] count1 = new int[]{0};
        final int[] count2 = new int[]{0};
        final int[] catchAllCount = new int[]{0};

        DefaultOutboundRouterCollection messageRouter = new DefaultOutboundRouterCollection();

        FilteringOutboundRouter filterRouter1 = new FilteringOutboundRouter()
        {
            public MuleMessage route(MuleMessage message, MuleSession session)
            {
                count1[0]++;
                return message;
            }
        };

        FilteringOutboundRouter filterRouter2 = new FilteringOutboundRouter()
        {
            public MuleMessage route(MuleMessage message, MuleSession session)
            {
                count2[0]++;
                return message;
            }
        };

        filterRouter1.setFilter(new PayloadTypeFilter(Exception.class));
        filterRouter2.setFilter(new PayloadTypeFilter(StringBuffer.class));
        messageRouter.addRouter(filterRouter1);
        messageRouter.addRouter(filterRouter2);

        AbstractCatchAllStrategy strategy = new AbstractCatchAllStrategy()
        {
            public MuleMessage doCatchMessage(MuleMessage message, MuleSession session)
            {
                catchAllCount[0]++;
                return null;
            }
        };
        messageRouter.setCatchAllStrategy(strategy);

        MuleSession session = getTestSession(getTestService(), muleContext);

        messageRouter.route(new DefaultMuleMessage("hello", muleContext), session);
        assertEquals(1, catchAllCount[0]);
        assertEquals(0, count1[0]);
        assertEquals(0, count2[0]);

        messageRouter.route(new DefaultMuleMessage(new StringBuffer(), muleContext), session);
        assertEquals(1, catchAllCount[0]);
        assertEquals(0, count1[0]);
        assertEquals(1, count2[0]);

        messageRouter.route(new DefaultMuleMessage(new Exception(), muleContext), session);
        assertEquals(1, catchAllCount[0]);
        assertEquals(1, count1[0]);
        assertEquals(1, count2[0]);
    }

}
