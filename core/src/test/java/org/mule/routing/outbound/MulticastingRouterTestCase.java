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
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.routing.filters.RegExFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.util.mock.PayloadConstraint;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;

public class MulticastingRouterTestCase extends AbstractMuleTestCase
{
    public MulticastingRouterTestCase()
    {
        setStartContext(true);
    }

    public void testMulticastingRouterAsync() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());
        RegExFilter filter = new RegExFilter("(.*) Message");

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://test1", null, filter, null);
        assertNotNull(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", "test://test2", null, filter, null);
        assertNotNull(endpoint2);
        Mock mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        Mock mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);

        MulticastingRouter router = createObject(MulticastingRouter.class);

        List<OutboundEndpoint> endpoints = new ArrayList<OutboundEndpoint>();
        endpoints.add((OutboundEndpoint) mockendpoint1.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint2.proxy());
        router.setEndpoints(endpoints);

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);

        assertTrue(router.isMatch(message));

        mockendpoint1.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        mockendpoint2.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        router.route(new OutboundRoutingTestEvent(message, (MuleSession)session.proxy()));
        mockendpoint1.verify();
        mockendpoint2.verify();

    }

    public void testMulticastingRouterSync() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://Test1Provider?synchronous=true");
        assertNotNull(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", "test://Test2Provider?synchronous=true");
        assertNotNull(endpoint2);
        Mock mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        Mock mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);

        MulticastingRouter router = createObject(MulticastingRouter.class);
        RegExFilter filter = new RegExFilter("(.*) Message");
        router.setFilter(filter);
        List<OutboundEndpoint> endpoints = new ArrayList<OutboundEndpoint>();
        endpoints.add((OutboundEndpoint) mockendpoint1.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint2.proxy());
        router.setEndpoints(endpoints);

        assertEquals(filter, router.getFilter());

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);

        assertTrue(router.isMatch(message));

        MuleEvent event = new OutboundRoutingTestEvent(message, null);

        mockendpoint1.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint2.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        MuleMessage result = router.route(new OutboundRoutingTestEvent(message, (MuleSession)session.proxy()));
        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        assertEquals(2, ((MuleMessageCollection)result).size());
        mockendpoint1.verify();
        mockendpoint2.verify();
    }

    public void testMulticastingRouterMixedSyncAsync() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://Test1Provider?synchronous=true");
        assertNotNull(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", "test://Test2Provider?synchronous=false");
        assertNotNull(endpoint2);

        Mock mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        Mock mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);

        MulticastingRouter router = createObject(MulticastingRouter.class);
        
        List<OutboundEndpoint> endpoints = new ArrayList<OutboundEndpoint>();
        endpoints.add((OutboundEndpoint) mockendpoint1.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint2.proxy());
        router.setEndpoints(endpoints);


        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);

        assertTrue(router.isMatch(message));
        MuleEvent event = new OutboundRoutingTestEvent(message, null);

        mockendpoint1.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint2.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        MuleMessage result = router.route(new OutboundRoutingTestEvent(message, (MuleSession)session.proxy()));
        assertNotNull(result);
        assertEquals(message, result);
        mockendpoint1.verify();
        mockendpoint2.verify();
    }
}
