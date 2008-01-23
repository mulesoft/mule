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
import org.mule.api.endpoint.Endpoint;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.endpoint.DynamicEndpointURIEndpoint;
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

        Endpoint endpoint1 = getTestEndpoint("Test1Provider", Endpoint.ENDPOINT_TYPE_SENDER);
        assertNotNull(endpoint1);

        Endpoint endpoint2 = getTestEndpoint("Test2Provider", Endpoint.ENDPOINT_TYPE_SENDER);
        assertNotNull(endpoint2);

        PayloadTypeFilter filter = new PayloadTypeFilter(String.class);
        router.setFilter(filter);
        endpoints = new ArrayList();
        endpoints.add(endpoint1);
        endpoints.add(endpoint2);
        router.setEndpoints(endpoints);

        assertEquals(filter, router.getFilter());
    }

    public void testChainingOutboundRouterSynchronous() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test event");
        assertTrue(router.isMatch(message));

        message = new DefaultMuleMessage("test event");

        session.expectAndReturn("sendEvent", C.eq(message, endpoints.get(0)), message);
        session.expectAndReturn("sendEvent", C.eq(message, endpoints.get(1)), message);
        final MuleMessage result = router.route(message, (MuleSession)session.proxy(), true);
        assertNotNull("This is a sync call, we need a result returned.", result);
        assertEquals(message, result);
        session.verify();
    }

    public void testChainingOutboundRouterSynchronousWithTemplate() throws Exception
    {
        Endpoint endpoint3 = getTestEndpoint("Test3Provider", Endpoint.ENDPOINT_TYPE_SENDER);
        assertNotNull(endpoint3);
        endpoint3.setEndpointURI(new MuleEndpointURI("test://foo?[barValue]"));
        router.addEndpoint(endpoint3);

        Map m = new HashMap();
        m.put("barValue", "bar");
        MuleMessage message = new DefaultMuleMessage("test event", m);
        assertTrue(router.isMatch(message));

        ImmutableEndpoint ep = router.getEndpoint(2, message);
        assertEquals("test://foo?bar", ep.getEndpointURI().toString());

        session.expectAndReturn("sendEvent", C.eq(message, new DynamicEndpointURIEndpoint(
            (ImmutableEndpoint) router.getEndpoints().get(0), new MuleEndpointURI("test://test"))), message);
        session.expectAndReturn("sendEvent", C.eq(message, new DynamicEndpointURIEndpoint(
            (ImmutableEndpoint) router.getEndpoints().get(1), new MuleEndpointURI("test://test"))), message);
        session.expectAndReturn("sendEvent", C.eq(message, new DynamicEndpointURIEndpoint(
            (ImmutableEndpoint) router.getEndpoints().get(2), new MuleEndpointURI("test://foo?bar"))), message);
        final MuleMessage result = router.route(message, (MuleSession)session.proxy(), true);
        assertNotNull("This is a sync call, we need a result returned.", result);
        assertEquals(message, result);
        session.verify();
    }

    public void testChainingOutboundRouterAsynchronous() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test event");
        assertTrue(router.isMatch(message));

        message = new DefaultMuleMessage("test event");

        session.expectAndReturn("sendEvent", C.eq(message, endpoints.get(0)), message);
        session.expectAndReturn("dispatchEvent", C.eq(message, endpoints.get(1)), message);
        final MuleMessage result = router.route(message, (MuleSession)session.proxy(), false);
        assertNull("Async call shouldn't return any result.", result);
        session.verify();
    }

    /**
     * One of the endpoints returns null and breaks the chain
     */
    public void testBrokenChain() throws Exception
    {
        final MuleMessage message = new DefaultMuleMessage("test event");
        final Endpoint endpoint1 = (Endpoint)endpoints.get(0);
        session.expect("sendEvent", C.eq(message, endpoint1));
        MuleMessage result = router.route(message, (MuleSession)session.proxy(), false);
        session.verify();
        assertNull(result);
    }

}
