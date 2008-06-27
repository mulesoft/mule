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
import org.mule.routing.filters.RegExFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;

public class MulticastingRouterTestCase extends AbstractMuleTestCase
{
    public void testMulticastingRouter() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getService", getTestService());
        
        ImmutableEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider");
        assertNotNull(endpoint1);

        ImmutableEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider");
        assertNotNull(endpoint2);

        MulticastingRouter router = new MulticastingRouter();
        RegExFilter filter = new RegExFilter("(.*) event");
        router.setFilter(filter);
        List endpoints = new ArrayList();
        endpoints.add(endpoint1);
        endpoints.add(endpoint2);
        router.setEndpoints(endpoints);

        assertEquals(filter, router.getFilter());

        MuleMessage message = new DefaultMuleMessage("test event");

        assertTrue(router.isMatch(message));

        session.expect("dispatchEvent", C.eq(message, endpoint1));
        session.expect("dispatchEvent", C.eq(message, endpoint2));
        router.route(message, (MuleSession)session.proxy(), false);
        session.verify();

        message = new DefaultMuleMessage("test event");

        session.expectAndReturn("sendEvent", C.eq(message, endpoint1), message);
        session.expectAndReturn("sendEvent", C.eq(message, endpoint2), message);
        MuleMessage result = router.route(message, (MuleSession)session.proxy(), true);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();
    }
}
