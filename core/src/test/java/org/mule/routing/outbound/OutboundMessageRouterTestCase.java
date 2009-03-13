/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.RoutingException;
import org.mule.routing.AbstractCatchAllStrategy;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.transport.DefaultMessageAdapter;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;

public class OutboundMessageRouterTestCase extends AbstractMuleTestCase
{
    public void testOutboundMessageRouter() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getService", getTestService());
        
        DefaultOutboundRouterCollection messageRouter = new DefaultOutboundRouterCollection();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());
        assertNotNull(messageRouter.getCatchAllStrategy());

        ImmutableEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://Test1Provider?synchronous=false");
        assertNotNull(endpoint1);

        ImmutableEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider");
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

        MuleMessage message = new DefaultMuleMessage("test event");

        session.expect("dispatchEvent", C.eq(message, endpoint1));
        messageRouter.route(message, (MuleSession)session.proxy());
        session.verify();

        message = new DefaultMuleMessage(new IllegalArgumentException());

        session.expectAndReturn("getService", getTestService());
        session.expect("dispatchEvent", C.eq(message, endpoint2));
        messageRouter.route(message, (MuleSession)session.proxy());
        session.verify();

        FilteringOutboundRouter router3 = new FilteringOutboundRouter();
        router3.setFilter(new PayloadTypeFilter(Object.class));
        endpoints = new ArrayList();
        endpoints.add(endpoint2);
        router3.setEndpoints(endpoints);
        messageRouter.addRouter(router3);

        // now the message should be routed twice to different endpoints
        message = new DefaultMuleMessage("testing multiple routing");
        session.expectAndReturn("getService", getTestService());
        session.expectAndReturn("getService", getTestService());

        session.expect("dispatchEvent", C.args(C.isA(MuleMessage.class), C.eq(endpoint1)));
        session.expect("dispatchEvent", C.args(C.isA(MuleMessage.class), C.eq(endpoint2)));

        messageRouter.setMatchAll(true);
        messageRouter.route(message, (MuleSession)session.proxy());
        session.verify();
    }

    public void testRouterWithCatchAll() throws Exception
    {
        final int[] count1 = new int[]{0};
        final int[] count2 = new int[]{0};
        final int[] catchAllCount = new int[]{0};

        DefaultOutboundRouterCollection messageRouter = new DefaultOutboundRouterCollection();

        FilteringOutboundRouter filterRouter1 = new FilteringOutboundRouter()
        {
            public MuleMessage route(MuleMessage message, MuleSession session)
                throws RoutingException
            {
                count1[0]++;
                return message;
            }
        };

        FilteringOutboundRouter filterRouter2 = new FilteringOutboundRouter()
        {
            public MuleMessage route(MuleMessage message, MuleSession session)
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

        AbstractCatchAllStrategy strategy = new AbstractCatchAllStrategy()
        {
            public MuleMessage doCatchMessage(MuleMessage message, MuleSession session) throws RoutingException
            {
                catchAllCount[0]++;
                return null;
            }
        };

        messageRouter.setCatchAllStrategy(strategy);

        MuleSession session = getTestSession(getTestService(), muleContext);

        messageRouter.route(new DefaultMuleMessage("hello"), session);
        assertEquals(1, catchAllCount[0]);
        assertEquals(0, count1[0]);
        assertEquals(0, count2[0]);

        messageRouter.route(new DefaultMuleMessage(new StringBuffer()), session);
        assertEquals(1, catchAllCount[0]);
        assertEquals(0, count1[0]);
        assertEquals(1, count2[0]);

        messageRouter.route(new DefaultMuleMessage(new Exception()), session);
        assertEquals(1, catchAllCount[0]);
        assertEquals(1, count1[0]);
        assertEquals(1, count2[0]);
    }

    public void testCorrelation() throws Exception
    {
        FilteringOutboundRouter filterRouter = new FilteringOutboundRouter();
        MuleSession session = getTestSession(getTestService(), muleContext);
        MuleMessage message = new DefaultMuleMessage(new DefaultMessageAdapter(new StringBuffer()));
        OutboundEndpoint endpoint = getTestOutboundEndpoint("test");
        filterRouter.setMessageProperties(session, message, endpoint);
        assertNotNull(message.getCorrelationId());
    }
}
