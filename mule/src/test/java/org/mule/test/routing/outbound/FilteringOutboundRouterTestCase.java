/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.routing.outbound;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.routing.outbound.OutboundMessageRouter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.transformer.TransformerException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class FilteringOutboundRouterTestCase extends AbstractMuleTestCase
{
    public void testFilteringOutboundRouter() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        OutboundMessageRouter messageRouter = new OutboundMessageRouter();
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
        router.route(message, (UMOSession) session.proxy(), false);
        session.verify();

        message = new MuleMessage("test event");

        session.expectAndReturn("sendEvent", C.eq(message, endpoint1), message);
        UMOMessage result = router.route(message, (UMOSession) session.proxy(), true);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();

        message = new MuleMessage(new Exception("test event"));

        assertTrue(!router.isMatch(message));

        router.setTransformer(new AbstractTransformer() {
            public Object doTransform(Object src, String encoding) throws TransformerException
            {
                return ((Exception) src).getMessage();
            }
        });

        assertTrue(router.isMatch(message));
    }

    public void testFilteringOutboundRouterWithTemplates() throws Exception
    {
        OutboundMessageRouter messageRouter = new OutboundMessageRouter();
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
