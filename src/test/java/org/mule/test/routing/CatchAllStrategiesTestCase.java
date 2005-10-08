/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.routing;

import com.mockobjects.constraint.Constraint;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.routing.ForwardingCatchAllStrategy;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.routing.outbound.OutboundMessageRouter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformers.NoActionTransformer;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.transformer.TransformerException;

import java.util.HashMap;

/**
 * <code>CatchAllStrategiesTestCase</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class CatchAllStrategiesTestCase extends AbstractMuleTestCase
{
    public void testLoggingOnlyStrategy() throws Exception
    {
        UMOEvent event = getTestEvent("UncaughtEvent");
        LoggingCatchAllStrategy strategy = new LoggingCatchAllStrategy();
        try {
            strategy.setEndpoint(getTestEndpoint("testProvider", UMOEndpoint.ENDPOINT_TYPE_SENDER));
            fail("Illegal operation exception shold have been thrown");
        } catch (Exception e) {
            // expected
        }

        assertNull(strategy.getEndpoint());
        strategy.catchMessage(event.getMessage(), null, false);
    }

    public void testForwardingStrategy() throws Exception
    {
        ForwardingCatchAllStrategy strategy = new ForwardingCatchAllStrategy();
        Mock endpoint = getMockEndpoint();
        Mock dispatcher = new Mock(UMOMessageDispatcher.class);
        Mock connector = getMockConnector();
        UMOEvent event = getTestEvent("UncaughtEvent");
        strategy.setEndpoint((UMOEndpoint) endpoint.proxy());

        endpoint.expectAndReturn("getProperties", new HashMap());
        endpoint.expectAndReturn("getProperties", new HashMap());
        endpoint.expectAndReturn("getConnector", connector.proxy());
        endpoint.expectAndReturn("getEndpointURI", new MuleEndpointURI("test://dummy"));
        endpoint.expectAndReturn("getEndpointURI", new MuleEndpointURI("test://dummy"));
        connector.expectAndReturn("getDispatcher", "dummy", dispatcher.proxy());
        dispatcher.expect("dispatch", C.isA(UMOEvent.class));
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
        public Object doTransform(Object src) throws TransformerException
        {
            return "Transformed Test Data";
        }
    }
    
    public void testForwardingStrategyWithTransform() throws Exception
    {
        ForwardingCatchAllStrategy strategy = new ForwardingCatchAllStrategy();
        strategy.setSendTransformed(true);
        Mock endpoint = getMockEndpoint();
        Mock dispatcher = new Mock(UMOMessageDispatcher.class);
        Mock connector = getMockConnector();
        UMOEvent event = getTestEvent("UncaughtEvent");
        strategy.setEndpoint((UMOEndpoint) endpoint.proxy());

        endpoint.expectAndReturn("getTransformer", new TestEventTransformer());
        endpoint.expectAndReturn("getTransformer", new TestEventTransformer());
        endpoint.expectAndReturn("getProperties", new HashMap());
        endpoint.expectAndReturn("getProperties", new HashMap());
        endpoint.expectAndReturn("getConnector", connector.proxy());
        endpoint.expectAndReturn("getEndpointURI", new MuleEndpointURI("test://dummy"));
        endpoint.expectAndReturn("getEndpointURI", new MuleEndpointURI("test://dummy"));
        connector.expectAndReturn("getDispatcher", "dummy", dispatcher.proxy());
        dispatcher.expect("send", new Constraint() {
            public boolean eval(Object arg0)
            {
                if (arg0 instanceof UMOEvent) {
                    return "Transformed Test Data".equals(((UMOEvent) arg0).getMessage().getPayload());
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
        final int[] count1 = new int[] { 0 };
        final int[] count2 = new int[] { 0 };
        final int[] catchAllCount = new int[] { 0 };

        OutboundMessageRouter messageRouter = new OutboundMessageRouter();

        FilteringOutboundRouter filterRouter1 = new FilteringOutboundRouter() {
            public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous)
                    throws RoutingException
            {
                count1[0]++;
                return message;
            }
        };

        FilteringOutboundRouter filterRouter2 = new FilteringOutboundRouter() {
            public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous)
                    throws RoutingException
            {
                count2[0]++;
                return message;
            }
        };

        filterRouter1.setFilter(new PayloadTypeFilter(Exception.class));
        filterRouter2.setFilter(new PayloadTypeFilter(StringBuffer.class));
        messageRouter.addRouter(filterRouter1);
        messageRouter.addRouter(filterRouter2);

        LoggingCatchAllStrategy strategy = new LoggingCatchAllStrategy() {
            public UMOMessage catchMessage(UMOMessage message, UMOSession session, boolean synchronous)
                    throws RoutingException
            {
                catchAllCount[0]++;
                return null;
            }
        };
        messageRouter.setCatchAllStrategy(strategy);

        UMOSession session = getTestSession(getTestComponent(getTestDescriptor("test", "test")));

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
