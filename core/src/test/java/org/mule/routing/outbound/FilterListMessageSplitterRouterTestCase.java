/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.MuleEventCheckAnswer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class FilterListMessageSplitterRouterTestCase extends AbstractMuleContextTestCase
{
    private MuleSession session = mock(MuleSession.class);
    private List<Object> payload;

    public FilterListMessageSplitterRouterTestCase()
    {
        setStartContext(true);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        payload = new ArrayList<Object>();
        payload.add(new Apple());
        payload.add(new Apple());
        payload.add(new Orange());
        payload.add(new String());
    }

    @Test
    public void testMessageSplitterRouterOneWay() throws Exception
    {
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1endpoint", "test://endpointUri.1", null, new PayloadTypeFilter(Apple.class), null);
        OutboundEndpoint mockendpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Endpoint", "test://endpointUri.2", null, new PayloadTypeFilter(Orange.class), null);
        OutboundEndpoint mockendpoint2 = RouterTestUtils.createMockEndpoint(endpoint2);

        OutboundEndpoint endpoint3 = getTestOutboundEndpoint("Test3Endpoint", "test://endpointUri.3");
        OutboundEndpoint mockendpoint3 = RouterTestUtils.createMockEndpoint(endpoint3);

        ListMessageSplitter router = createObject(ListMessageSplitter.class);
        router.setFilter(new PayloadTypeFilter(List.class));
        router.addRoute(mockendpoint1);
        router.addRoute(mockendpoint2);
        router.addRoute(mockendpoint3);

        MuleMessage message = new DefaultMuleMessage(payload, muleContext);
        assertTrue(router.isMatch(message));

        when(mockendpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());
        when(mockendpoint2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());
        when(mockendpoint3.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());
        router.route(new OutboundRoutingTestEvent(message, session, muleContext));
    }

    @Test
    public void testMessageSplitterRouterRequestResponse() throws Exception
    {
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1endpoint",
            "test://endpointUri.1?exchangePattern=request-response", null,
            new PayloadTypeFilter(Apple.class), null);
        OutboundEndpoint mockendpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Endpoint",
            "test://endpointUri.2?exchangePattern=request-response", null,
            new PayloadTypeFilter(Orange.class), null);
        OutboundEndpoint mockendpoint2 = RouterTestUtils.createMockEndpoint(endpoint2);

        OutboundEndpoint endpoint3 = getTestOutboundEndpoint("Test3Endpoint",
            "test://endpointUri.3?exchangePattern=request-response");
        OutboundEndpoint mockendpoint3 = RouterTestUtils.createMockEndpoint(endpoint3);

        ListMessageSplitter router = createObject(ListMessageSplitter.class);
        router.setFilter(new PayloadTypeFilter(List.class));
        router.addRoute(mockendpoint1);
        router.addRoute(mockendpoint2);
        router.addRoute(mockendpoint3);

        MuleMessage message = new DefaultMuleMessage(payload, muleContext);
        MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);

        when(mockendpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));
        when(mockendpoint2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));
        when(mockendpoint3.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));

        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNotNull(result);
        MuleMessage resultMessage = result.getMessage();
        assertNotNull(resultMessage);
        assertTrue(resultMessage.getPayload() instanceof List);
        assertEquals(((List<?>) resultMessage.getPayload()).size(), 4);
    }
}
