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

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.routing.outbound.OutboundRouterCollection;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.CollectionUtils;

import com.mockobjects.constraint.Constraint;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.HashMap;

public class CatchAllStrategiesTestCase extends AbstractMuleTestCase
{

    public void testLoggingOnlyStrategy() throws Exception
    {
        UMOEvent event = getTestEvent("UncaughtEvent");
        LoggingCatchAllStrategy strategy = new LoggingCatchAllStrategy();
        try
        {
            strategy.setEndpoint(getTestEndpoint("testProvider", UMOEndpoint.ENDPOINT_TYPE_SENDER));
            fail("Illegal operation exception should have been thrown");
        }
        catch (Exception e)
        {
            // expected
        }

        assertNull(strategy.getEndpoint());
        strategy.catchMessage(event.getMessage(), null, false);
    }

    public void testForwardingStrategy() throws Exception
    {
        ForwardingCatchAllStrategy strategy = new ForwardingCatchAllStrategy();
        Mock endpoint = MuleTestUtils.getMockEndpoint();
        Mock dispatcher = new Mock(UMOMessageDispatcher.class);
        Mock connector = MuleTestUtils.getMockConnector();
        UMOEvent event = getTestEvent("UncaughtEvent");
        strategy.setEndpoint((UMOEndpoint)endpoint.proxy());

        endpoint.expectAndReturn("getProperties", new HashMap());
        endpoint.expectAndReturn("getProperties", new HashMap());
        endpoint.expectAndReturn("getEndpointURI", new MuleEndpointURI("test://dummy"));
        endpoint.expectAndReturn("getEndpointURI", new MuleEndpointURI("test://dummy"));
        endpoint.expect("dispatch", C.isA(MuleEvent.class));

        strategy.catchMessage(event.getMessage(), null, false);

        endpoint.verify();
        dispatcher.verify();
        connector.verify();

        assertNotNull(strategy.getEndpoint());
    }

    private class TestEventTransformer extends AbstractTransformer
    {
        /*
         * (non-Javadoc)
         * 
         * @see org.mule.transformers.AbstractTransformer#doTransform(java.lang.Object)
         */
        public Object doTransform(Object src, String encoding) throws TransformerException
        {
            return "Transformed Test Data";
        }
    }

    public void testForwardingStrategyWithTransform() throws Exception
    {
        ForwardingCatchAllStrategy strategy = new ForwardingCatchAllStrategy();
        strategy.setSendTransformed(true);
        Mock endpoint = MuleTestUtils.getMockEndpoint();
        Mock dispatcher = new Mock(UMOMessageDispatcher.class);
        Mock connector = MuleTestUtils.getMockConnector();
        UMOEvent event = getTestEvent("UncaughtEvent");
        strategy.setEndpoint((UMOEndpoint)endpoint.proxy());

        endpoint.expectAndReturn("getTransformers", CollectionUtils.singletonList(new TestEventTransformer()));
        endpoint.expectAndReturn("getTransformers", CollectionUtils.singletonList(new TestEventTransformer()));
        endpoint.expectAndReturn("getProperties", new HashMap());
        endpoint.expectAndReturn("getProperties", new HashMap());
        endpoint.expectAndReturn("getEndpointURI", new MuleEndpointURI("test://dummy"));
        endpoint.expectAndReturn("getEndpointURI", new MuleEndpointURI("test://dummy"));
        endpoint.expect("send", new Constraint()
        {
            public boolean eval(Object arg0)
            {
                if (arg0 instanceof UMOEvent)
                {
                    return "Transformed Test Data".equals(((UMOEvent)arg0).getMessage().getPayload());
                }
                return false;
            }
        });

        strategy.catchMessage(event.getMessage(), null, true);

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

        OutboundRouterCollection messageRouter = new OutboundRouterCollection();

        FilteringOutboundRouter filterRouter1 = new FilteringOutboundRouter()
        {
            public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous)
            {
                count1[0]++;
                return message;
            }
        };

        FilteringOutboundRouter filterRouter2 = new FilteringOutboundRouter()
        {
            public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous)
            {
                count2[0]++;
                return message;
            }
        };

        filterRouter1.setFilter(new PayloadTypeFilter(Exception.class));
        filterRouter2.setFilter(new PayloadTypeFilter(StringBuffer.class));
        messageRouter.addRouter(filterRouter1);
        messageRouter.addRouter(filterRouter2);

        LoggingCatchAllStrategy strategy = new LoggingCatchAllStrategy()
        {
            public UMOMessage catchMessage(UMOMessage message, UMOSession session, boolean synchronous)
            {
                catchAllCount[0]++;
                return null;
            }
        };
        messageRouter.setCatchAllStrategy(strategy);

        UMOSession session = getTestSession(getTestComponent());

        messageRouter.route(new MuleMessage("hello"), session, true);
        assertEquals(1, catchAllCount[0]);
        assertEquals(0, count1[0]);
        assertEquals(0, count2[0]);

        messageRouter.route(new MuleMessage(new StringBuffer()), session, true);
        assertEquals(1, catchAllCount[0]);
        assertEquals(0, count1[0]);
        assertEquals(1, count2[0]);

        messageRouter.route(new MuleMessage(new Exception()), session, true);
        assertEquals(1, catchAllCount[0]);
        assertEquals(1, count1[0]);
        assertEquals(1, count2[0]);
    }

}
