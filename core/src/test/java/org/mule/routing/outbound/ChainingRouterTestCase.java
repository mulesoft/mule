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
import org.mule.endpoint.DynamicURIOutboundEndpoint;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChainingRouterTestCase extends AbstractMuleTestCase
{
    private Mock session;
    private ChainingRouter router;
    private List endpoints;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        session = MuleTestUtils.getMockSession();
        router = new ChainingRouter();

        DefaultOutboundRouterCollection messageRouter = new DefaultOutboundRouterCollection();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        ImmutableEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://test?synchronous=true");
        assertNotNull(endpoint1);

        ImmutableEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", "test://test?synchronous=true");
        assertNotNull(endpoint2);

        PayloadTypeFilter filter = new PayloadTypeFilter(String.class);
        router.setFilter(filter);
        endpoints = new ArrayList();
        endpoints.add(endpoint1);
        endpoints.add(endpoint2);
        router.setEndpoints(endpoints);

        assertEquals(filter, router.getFilter());
        session.matchAndReturn("getService", getTestService("TEST", Object.class));    
    }

    public void testChainingOutboundRouterSynchronous() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test event");
        assertTrue(router.isMatch(message));

        message = new DefaultMuleMessage("test event");

        session.expectAndReturn("sendEvent", C.eq(message, endpoints.get(0)), message);
        session.expectAndReturn("sendEvent", C.eq(message, endpoints.get(1)), message);
        final MuleMessage result = router.route(message, (MuleSession)session.proxy());
        assertNotNull("This is a sync call, we need a result returned.", result);
        assertEquals(message, result);
        session.verify();
    }

    public void testChainingOutboundRouterSynchronousWithTemplate() throws Exception
    {
        OutboundEndpoint endpoint3 = getTestOutboundEndpoint("Test3Provider", "test://foo?[barValue]&synchronous=true");
        assertNotNull(endpoint3);
        router.addEndpoint(endpoint3);

        Map m = new HashMap();
        m.put("barValue", "bar");
        MuleMessage message = new DefaultMuleMessage("test event", m);
        assertTrue(router.isMatch(message));

        ImmutableEndpoint ep = router.getEndpoint(2, message);
        assertEquals("test://foo?bar&synchronous=true", ep.getEndpointURI().toString());

        session.expectAndReturn("sendEvent", C.eq(message, new DynamicURIOutboundEndpoint(
            (OutboundEndpoint) router.getEndpoints().get(0), new MuleEndpointURI("test://test?synchronous=true"))), message);
        session.expectAndReturn("sendEvent", C.eq(message, new DynamicURIOutboundEndpoint(
            (OutboundEndpoint) router.getEndpoints().get(1), new MuleEndpointURI("test://test?synchronous=true"))), message);
        session.expectAndReturn("sendEvent", C.eq(message, new DynamicURIOutboundEndpoint(
            (OutboundEndpoint) router.getEndpoints().get(2), new MuleEndpointURI("test://foo?bar&synchronous=true"))), message);
        final MuleMessage result = router.route(message, (MuleSession)session.proxy());
        assertNotNull("This is a sync call, we need a result returned.", result);
        assertEquals(message, result);
        session.verify();
    }

    public void testChainingOutboundRouterAsynchronous() throws Exception
    {
        ImmutableEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://test");
        assertNotNull(endpoint1);

        ImmutableEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", "test://test");
        assertNotNull(endpoint2);

        endpoints.clear();
        endpoints.add(endpoint1);
        endpoints.add(endpoint2);
        router.setEndpoints(endpoints);
        MuleMessage message = new DefaultMuleMessage("test event");
        assertTrue(router.isMatch(message));

        message = new DefaultMuleMessage("test event");

        session.expectAndReturn("sendEvent", C.eq(message, endpoints.get(0)), message);
        session.expectAndReturn("dispatchEvent", C.eq(message, endpoints.get(1)), message);
        final MuleMessage result = router.route(message, (MuleSession)session.proxy());
        assertNull("Async call shouldn't return any result.", result);
        session.verify();
    }

    /**
     * One of the endpoints returns null and breaks the chain
     */
    public void testBrokenChain() throws Exception
    {
        final MuleMessage message = new DefaultMuleMessage("test event");
        final ImmutableEndpoint endpoint1 = (ImmutableEndpoint)endpoints.get(0);
        session.expect("sendEvent", C.eq(message, endpoint1));
        MuleMessage result = router.route(message, (MuleSession)session.proxy());
        session.verify();
        assertNull(result);
    }

}
