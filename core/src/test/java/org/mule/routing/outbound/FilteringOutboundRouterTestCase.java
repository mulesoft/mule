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

import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.transformer.TransformerException;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilteringOutboundRouterTestCase extends AbstractMuleTestCase
{
    public void testFilteringOutboundRouter() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        OutboundRouterCollection messageRouter = new OutboundRouterCollection();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        UMOEndpoint endpoint1 = getTestEndpoint("Test1Provider", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        assertNotNull(endpoint1);

        FilteringOutboundRouter router = new FilteringOutboundRouter();
        PayloadTypeFilter filter = new PayloadTypeFilter(String.class);
        router.setFilter(filter);
        List endpoints = new ArrayList();
        endpoints.add(endpoint1);
        router.setEndpoints(endpoints);

        assertFalse(router.isUseTemplates());
        assertEquals(filter, router.getFilter());

        UMOMessage message = new MuleMessage("test event");

        assertTrue(router.isMatch(message));

        session.expect("dispatchEvent", C.eq(message, endpoint1));
        router.route(message, (UMOSession)session.proxy(), false);
        session.verify();

        message = new MuleMessage("test event");

        session.expectAndReturn("sendEvent", C.eq(message, endpoint1), message);
        UMOMessage result = router.route(message, (UMOSession)session.proxy(), true);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();

        message = new MuleMessage(new Exception("test event"));

        assertTrue(!router.isMatch(message));

        router.setTransformer(new AbstractTransformer()
        {
            public Object doTransform(Object src, String encoding) throws TransformerException
            {
                return ((Exception)src).getMessage();
            }
        });

        assertTrue(router.isMatch(message));
    }

    public void testFilteringOutboundRouterWithTemplates() throws Exception
    {
        OutboundRouterCollection messageRouter = new OutboundRouterCollection();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        UMOEndpoint endpoint1 = getTestEndpoint("Test1Provider", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        assertNotNull(endpoint1);
        endpoint1.setEndpointURI(new MuleEndpointURI("test://foo?[barValue]"));

        FilteringOutboundRouter router = new FilteringOutboundRouter();
        PayloadTypeFilter filter = new PayloadTypeFilter(String.class);
        router.setFilter(filter);
        List endpoints = new ArrayList();
        endpoints.add(endpoint1);
        router.setEndpoints(endpoints);

        assertTrue(router.isUseTemplates());
        assertEquals(filter, router.getFilter());

        Map m = new HashMap();
        m.put("barValue", "bar");
        UMOMessage message = new MuleMessage("test event", m);

        assertTrue(router.isMatch(message));
        UMOEndpoint ep = router.getEndpoint(0, message);
        assertEquals("test://foo?bar", ep.getEndpointURI().toString());
    }
}
