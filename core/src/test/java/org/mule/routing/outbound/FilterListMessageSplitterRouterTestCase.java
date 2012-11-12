/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;

import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class FilterListMessageSplitterRouterTestCase extends AbstractMuleContextTestCase
{
    public FilterListMessageSplitterRouterTestCase()
    {
        setStartContext(true);
    }

    @Test
    public void testMessageSplitterRouter() throws Exception
    {
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1endpoint", "test://endpointUri.1", null, new PayloadTypeFilter(Apple.class), null);
        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Endpoint", "test://endpointUri.2", null, new PayloadTypeFilter(Orange.class), null);
        OutboundEndpoint endpoint3 = getTestOutboundEndpoint("Test3Endpoint", "test://endpointUri.3");
        Mock mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        Mock mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);
        Mock mockendpoint3 = RouterTestUtils.getMockEndpoint(endpoint3);

        ListMessageSplitter router = createObject(ListMessageSplitter.class);
        router.setFilter(new PayloadTypeFilter(List.class));
        router.addRoute((OutboundEndpoint) mockendpoint1.proxy());
        router.addRoute((OutboundEndpoint) mockendpoint2.proxy());
        router.addRoute((OutboundEndpoint) mockendpoint3.proxy());

        List<Object> payload = new ArrayList<Object>();
        payload.add(new Apple());
        payload.add(new Apple());
        payload.add(new Orange());
        payload.add(new String());
        MuleMessage message = new DefaultMuleMessage(payload, muleContext);

        assertTrue(router.isMatch(message));
        mockendpoint1.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        mockendpoint1.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        mockendpoint2.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        mockendpoint3.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());

        MuleSession session = mock(MuleSession.class);
        router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        mockendpoint1.verify();
        mockendpoint2.verify();
        mockendpoint3.verify();

        endpoint1 = getTestOutboundEndpoint("Test1endpoint",
            "test://endpointUri.1?exchangePattern=request-response", null,
            new PayloadTypeFilter(Apple.class), null);
        endpoint2 = getTestOutboundEndpoint("Test2Endpoint",
            "test://endpointUri.2?exchangePattern=request-response", null,
            new PayloadTypeFilter(Orange.class), null);
        endpoint3 = getTestOutboundEndpoint("Test3Endpoint",
            "test://endpointUri.3?exchangePattern=request-response");
        mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);
        mockendpoint3 = RouterTestUtils.getMockEndpoint(endpoint3);
        router = createObject(ListMessageSplitter.class);
        router.setFilter(new PayloadTypeFilter(List.class));
        router.addRoute((OutboundEndpoint) mockendpoint1.proxy());
        router.addRoute((OutboundEndpoint) mockendpoint2.proxy());
        router.addRoute((OutboundEndpoint) mockendpoint3.proxy());

        message = new DefaultMuleMessage(payload, muleContext);

        MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);

        mockendpoint1.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint1.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint2.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint3.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNotNull(result);
        MuleMessage resultMessage = result.getMessage();
        assertNotNull(resultMessage);
        assertTrue(resultMessage.getPayload() instanceof List);
        assertEquals(((List<?>) resultMessage.getPayload()).size(), 4);
        mockendpoint1.verify();
        mockendpoint2.verify();
        mockendpoint3.verify();
    }
}
