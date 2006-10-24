/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing.outbound;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.outbound.ChainingRouter;
import org.mule.routing.outbound.OutboundMessageRouter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author <a href="mailto:aperepel@gmail.com">Andrew Perepelytsya</a>
 * @version $Revision$
 */

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

        OutboundMessageRouter messageRouter = new OutboundMessageRouter();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        UMOEndpoint endpoint1 = getTestEndpoint("Test1Provider", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        assertNotNull(endpoint1);

        UMOEndpoint endpoint2 = getTestEndpoint("Test2Provider", UMOEndpoint.ENDPOINT_TYPE_SENDER);
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
        UMOMessage message = new MuleMessage("test event");
        assertTrue(router.isMatch(message));

        message = new MuleMessage("test event");

        session.expectAndReturn("sendEvent", C.eq(message, endpoints.get(0)), message);
        session.expectAndReturn("sendEvent", C.eq(message, endpoints.get(1)), message);
        final UMOMessage result = router.route(message, (UMOSession)session.proxy(), true);
        assertNotNull("This is a sync call, we need a result returned.", result);
        assertEquals(message, result);
        session.verify();
    }

    public void testChainingOutboundRouterSynchronousWithTemplate() throws Exception
    {
        UMOEndpoint endpoint3 = getTestEndpoint("Test3Provider", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        assertNotNull(endpoint3);
        endpoint3.setEndpointURI(new MuleEndpointURI("test://foo?[barValue]"));
        router.addEndpoint(endpoint3);

        Map m = new HashMap();
        m.put("barValue", "bar");
        UMOMessage message = new MuleMessage("test event", m);
        assertTrue(router.isMatch(message));

        UMOEndpoint ep = router.getEndpoint(2, message);
        assertEquals("test://foo?bar", ep.getEndpointURI().toString());

        session.expectAndReturn("sendEvent", C.eq(message, router.getEndpoints().get(0)), message);
        session.expectAndReturn("sendEvent", C.eq(message, router.getEndpoints().get(1)), message);
        session.expectAndReturn("sendEvent", C.eq(message, router.getEndpoints().get(2)), message);
        final UMOMessage result = router.route(message, (UMOSession)session.proxy(), true);
        assertNotNull("This is a sync call, we need a result returned.", result);
        assertEquals(message, result);
        session.verify();
    }

    public void testChainingOutboundRouterAsynchronous() throws Exception
    {
        UMOMessage message = new MuleMessage("test event");
        assertTrue(router.isMatch(message));

        message = new MuleMessage("test event");

        session.expectAndReturn("sendEvent", C.eq(message, endpoints.get(0)), message);
        session.expectAndReturn("dispatchEvent", C.eq(message, endpoints.get(1)), message);
        final UMOMessage result = router.route(message, (UMOSession)session.proxy(), false);
        assertNull("Async call shouldn't return any result.", result);
        session.verify();
    }

    /**
     * One of the endpoints returns null and breaks the chain
     */
    public void testBrokenChain() throws Exception
    {
        final UMOMessage message = new MuleMessage("test event");
        final UMOEndpoint endpoint1 = (UMOEndpoint)endpoints.get(0);
        session.expect("sendEvent", C.eq(message, endpoint1));
        UMOMessage result = router.route(message, (UMOSession)session.proxy(), false);
        session.verify();
        assertNull(result);
    }

}
