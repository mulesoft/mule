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
        session.matchAndReturn("setFlowConstruct", RouterTestUtils.getArgListCheckerFlowConstruct(), null);

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://Test1Provider?exchange-pattern=request-response");
        assertNotNull(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", "test://Test2Provider?exchange-pattern=request-response");
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
        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, (MuleSession)session.proxy()));
        assertNotNull(result);
        MuleMessage resultMessage = result.getMessage();
        assertNotNull(resultMessage);
        assertTrue(resultMessage instanceof MuleMessageCollection);
        assertEquals(2, ((MuleMessageCollection)resultMessage).size());
        mockendpoint1.verify();
        mockendpoint2.verify();
    }

    public void testMulticastingRouterMixedSyncAsync() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());
        session.matchAndReturn("setFlowConstruct", RouterTestUtils.getArgListCheckerFlowConstruct(), null);

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://Test1Provider?exchange-pattern=request-response");
        assertNotNull(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", "test://Test2Provider?exchange-pattern=request-response");
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
        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, (MuleSession)session.proxy()));
        assertNotNull(result);
        assertEquals(getPayload(message), getPayload(result.getMessage()));
        mockendpoint1.verify();
        mockendpoint2.verify();
    }

    private String getPayload(MuleMessage message) throws Exception
    {
        Object payload = message.getPayload();
        if (payload instanceof List)
            payload = ((List)payload).get(0);
        return payload.toString();
    }
}
