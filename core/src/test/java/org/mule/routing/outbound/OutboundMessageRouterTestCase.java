/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.impl.MuleMessage;
import org.mule.providers.DefaultMessageAdapter;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.RoutingException;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;

public class OutboundMessageRouterTestCase extends AbstractMuleTestCase
{
    public void testOutboundMessageRouter() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.expectAndReturn("getComponent", getTestComponent(getTestDescriptor("test", "blah")));
        OutboundRouterCollection messageRouter = new OutboundRouterCollection();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());
        assertNotNull(messageRouter.getCatchAllStrategy());

        UMOEndpoint endpoint1 = getTestEndpoint("Test1Provider", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        assertNotNull(endpoint1);

        UMOEndpoint endpoint2 = getTestEndpoint("Test2Provider", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        assertNotNull(endpoint2);

        FilteringOutboundRouter router1 = new FilteringOutboundRouter();
        PayloadTypeFilter filter = new PayloadTypeFilter(String.class);
        router1.setFilter(filter);
        List endpoints = new ArrayList();
        endpoints.add(endpoint1);
        router1.setEndpoints(endpoints);

        FilteringOutboundRouter router2 = new FilteringOutboundRouter();
        PayloadTypeFilter filter2 = new PayloadTypeFilter();
        filter2.setExpectedType(Exception.class);
        router2.setFilter(filter2);
        endpoints = new ArrayList();
        endpoints.add(endpoint2);
        router2.setEndpoints(endpoints);

        messageRouter.addRouter(router1);
        assertEquals(1, messageRouter.getRouters().size());
        assertNotNull(messageRouter.removeRouter(router1));
        assertEquals(0, messageRouter.getRouters().size());
        List list = new ArrayList();
        list.add(router1);
        list.add(router2);
        messageRouter.setRouters(list);

        UMOMessage message = new MuleMessage("test event");

        session.expect("dispatchEvent", C.eq(message, endpoint1));
        messageRouter.route(message, (UMOSession)session.proxy(), false);
        session.verify();

        message = new MuleMessage(new IllegalArgumentException());

        session.expectAndReturn("getComponent", getTestComponent(getTestDescriptor("test", "blah")));
        session.expect("dispatchEvent", C.eq(message, endpoint2));
        messageRouter.route(message, (UMOSession)session.proxy(), false);
        session.verify();

        FilteringOutboundRouter router3 = new FilteringOutboundRouter();
        router3.setFilter(new PayloadTypeFilter(Object.class));
        endpoints = new ArrayList();
        endpoints.add(endpoint2);
        router3.setEndpoints(endpoints);
        messageRouter.addRouter(router3);

        // now the message should be routed twice to different endpoints
        message = new MuleMessage("testing multiple routing");
        session.expectAndReturn("getComponent", getTestComponent(getTestDescriptor("test", "blah")));
        session.expectAndReturn("getComponent", getTestComponent(getTestDescriptor("test", "blah")));

        session.expect("dispatchEvent", C.eq(message, endpoint1));
        session.expect("dispatchEvent", C.eq(message, endpoint2));
        messageRouter.setMatchAll(true);
        messageRouter.route(message, (UMOSession)session.proxy(), false);
        session.verify();
    }

    public void testRouterWithCatchAll() throws Exception
    {
        final int[] count1 = new int[]{0};
        final int[] count2 = new int[]{0};
        final int[] catchAllCount = new int[]{0};

        OutboundRouterCollection messageRouter = new OutboundRouterCollection();

        FilteringOutboundRouter filterRouter1 = new FilteringOutboundRouter()
        {
            public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous)
                throws RoutingException
            {
                count1[0]++;
                return message;
            }
        };

        FilteringOutboundRouter filterRouter2 = new FilteringOutboundRouter()
        {
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

        LoggingCatchAllStrategy strategy = new LoggingCatchAllStrategy()
        {
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

    public void testCorrelation() throws Exception
    {
        FilteringOutboundRouter filterRouter = new FilteringOutboundRouter();
        UMOSession session = getTestSession(getTestComponent(getTestDescriptor("test", "test")));
        UMOMessage message = new MuleMessage(new DefaultMessageAdapter(new StringBuffer()));
        UMOEndpoint endpoint = getTestEndpoint("test", "sender");
        filterRouter.setMessageProperties(session, message, endpoint);
        assertNotNull(message.getCorrelationId());
    }
}
