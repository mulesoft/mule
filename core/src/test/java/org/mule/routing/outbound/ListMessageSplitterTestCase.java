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
import org.mule.api.MuleMessageCollection;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.service.Service;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.MuleEventCheckAnswer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ListMessageSplitterTestCase extends AbstractMuleContextTestCase
{
    public ListMessageSplitterTestCase()
    {
        setStartContext(true);
    }

    @Test
    public void testCorrelationGroupSizePropertySet() throws Exception
    {
        Service testService = getTestService("test", Apple.class);
        MuleSession session = getTestSession(testService, muleContext);

        OutboundEndpoint endpoint = getTestOutboundEndpoint("Test1Endpoint",
            "test://endpoint?exchangePattern=request-response");
        ListMessageSplitter router = new ListMessageSplitter();
        router.setFilter(null);
        router.addRoute(endpoint);
        router.setMuleContext(muleContext);

        List<String> payload = new ArrayList<String>();
        payload.add("one");
        payload.add("two");
        payload.add("three");
        payload.add("four");

        MuleMessage message = new DefaultMuleMessage(payload, muleContext);

        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNotNull(result);
        MuleMessage resultMessage = result.getMessage();
        assertNotNull(resultMessage);
        assertTrue(resultMessage instanceof MuleMessageCollection);
        assertEquals("There should be 4 results for 4 split messages.", 4, ((MuleMessageCollection) resultMessage).size());
    }

    @Test
    public void testMessageSplitterRouter() throws Exception
    {
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1endpoint", "test://endpointUri.1", null, new PayloadTypeFilter(Apple.class), null);
        OutboundEndpoint mockendpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Endpoint", "test://endpointUri.2", null, new PayloadTypeFilter(Orange.class), null);
        OutboundEndpoint mockendpoint2 = RouterTestUtils.createMockEndpoint(endpoint2);

        OutboundEndpoint endpoint3 = getTestOutboundEndpoint("Test3Endpoint", "test://endpointUri.3");
        OutboundEndpoint mockendpoint3 = RouterTestUtils.createMockEndpoint(endpoint3);

        OutboundEndpoint endpoint4 = getTestOutboundEndpoint("Test4endpoint",
            "test://endpointUri.4?exchangePattern=request-response", null,
            new PayloadTypeFilter(Apple.class), null);
        OutboundEndpoint mockendpoint4 = RouterTestUtils.createMockEndpoint(endpoint4);

        OutboundEndpoint endpoint5 = getTestOutboundEndpoint("Test5Endpoint",
            "test://endpointUri.5?exchangePattern=request-response", null,
            new PayloadTypeFilter(Orange.class), null);
        OutboundEndpoint mockendpoint5 = RouterTestUtils.createMockEndpoint(endpoint5);

        OutboundEndpoint endpoint6 = getTestOutboundEndpoint("Test6Endpoint",
            "test://endpointUri.6?exchangePattern=request-response");
        OutboundEndpoint mockendpoint6 = RouterTestUtils.createMockEndpoint(endpoint6);

        ListMessageSplitter asyncSplitter = new ListMessageSplitter();
        asyncSplitter.setMuleContext(muleContext);
        asyncSplitter.setDisableRoundRobin(true);
        asyncSplitter.setFilter(new PayloadTypeFilter(List.class));
        asyncSplitter.addRoute(mockendpoint1);
        asyncSplitter.addRoute(mockendpoint2);
        asyncSplitter.addRoute(mockendpoint3);

        ListMessageSplitter syncSplitter = new ListMessageSplitter();
        syncSplitter.setMuleContext(muleContext);
        syncSplitter.setDisableRoundRobin(true);
        syncSplitter.setFilter(new PayloadTypeFilter(List.class));
        syncSplitter.addRoute(mockendpoint4);
        syncSplitter.addRoute(mockendpoint5);
        syncSplitter.addRoute(mockendpoint6);
        List<Object> payload = new ArrayList<Object>();
        payload.add(new Apple());
        payload.add(new Apple());
        payload.add(new Orange());
        payload.add("");
        MuleMessage message = new DefaultMuleMessage(payload, muleContext);

        assertTrue(asyncSplitter.isMatch(message));
        when(mockendpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());
        when(mockendpoint2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());
        when(mockendpoint3.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());

        MuleSession session = mock(MuleSession.class);
        asyncSplitter.route(new OutboundRoutingTestEvent(message, session, muleContext));

        message = new DefaultMuleMessage(payload, muleContext);
        MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);

        when(mockendpoint4.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));
        when(mockendpoint5.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));
        when(mockendpoint6.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));

        MuleEvent result = syncSplitter.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNotNull(result);
        MuleMessage resultMessage = result.getMessage();
        assertNotNull(resultMessage);
        assertTrue(resultMessage instanceof MuleMessageCollection);
        assertEquals(4, ((MuleMessageCollection) resultMessage).size());
    }
}
