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
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.RegExFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;

public class MulticastingRouterTestCase extends AbstractMuleTestCase
{
    public void testMulticastingRouter() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        OutboundRouterCollection messageRouter = new OutboundRouterCollection();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        UMOEndpoint endpoint1 = getTestEndpoint("Test1Provider", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        assertNotNull(endpoint1);

        UMOEndpoint endpoint2 = getTestEndpoint("Test2Provider", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        assertNotNull(endpoint2);

        MulticastingRouter router = new MulticastingRouter();
        RegExFilter filter = new RegExFilter("(.*) event");
        router.setFilter(filter);
        List endpoints = new ArrayList();
        endpoints.add(endpoint1);
        endpoints.add(endpoint2);
        router.setEndpoints(endpoints);

        assertEquals(filter, router.getFilter());

        UMOMessage message = new MuleMessage("test event");

        assertTrue(router.isMatch(message));

        session.expect("dispatchEvent", C.eq(message, endpoint1));
        session.expect("dispatchEvent", C.eq(message, endpoint2));
        router.route(message, (UMOSession)session.proxy(), false);
        session.verify();

        message = new MuleMessage("test event");

        session.expectAndReturn("sendEvent", C.eq(message, endpoint1), message);
        session.expectAndReturn("sendEvent", C.eq(message, endpoint2), message);
        UMOMessage result = router.route(message, (UMOSession)session.proxy(), true);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();
    }
}
