/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.MuleEventCheckAnswer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class ChainingRouterTestCase extends AbstractMuleContextTestCase
{
    private MuleSession session;
    private ChainingRouter router;
    private List<OutboundEndpoint> endpoints;
    private OutboundEndpoint mockEndpoint1;
    private OutboundEndpoint mockEndpoint2;
    private OutboundEndpoint mockEndpoint3;

    public ChainingRouterTestCase()
    {
        setStartContext(true);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        session = mock(MuleSession.class);
        router = new ChainingRouter();
        router.setMuleContext(muleContext);

        DefaultOutboundRouterCollection messageRouter = new DefaultOutboundRouterCollection();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider",
            "test://test?exchangePattern=request-response");
        assertNotNull(endpoint1);
        mockEndpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider",
            "test://test?exchangePattern=request-response");
        assertNotNull(endpoint2);
        mockEndpoint2 = RouterTestUtils.createMockEndpoint(endpoint2);

        PayloadTypeFilter filter = new PayloadTypeFilter(String.class);
        router.setFilter(filter);

        endpoints = new ArrayList<OutboundEndpoint>();
        endpoints.add(mockEndpoint1);
        endpoints.add(mockEndpoint2);
        router.setRoutes(new ArrayList<MessageProcessor>(endpoints));

        assertEquals(filter, router.getFilter());
    }

    @Test
    public void testChainingOutboundRouterSynchronous() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test event", muleContext);
        assertTrue(router.isMatch(message));

        MuleEvent responseEvent = new OutboundRoutingTestEvent(message, session, muleContext);
        when(mockEndpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(responseEvent));
        when(mockEndpoint2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(responseEvent));

        final MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNotNull("This is a sync call, we need a result returned.", result);
        assertEquals(message, result.getMessage());
    }

    @Test
    public void testChainingOutboundRouterSynchronousWithTemplate() throws Exception
    {
        OutboundEndpoint endpoint3 = getTestOutboundEndpoint("Test3Provider",
            "test://foo?[barValue]&exchangePattern=request-response");
        assertNotNull(endpoint3);
        mockEndpoint3 = RouterTestUtils.createMockEndpoint(endpoint3);
        router.addRoute(mockEndpoint3);

        Map<String, Object> messageProperties = new HashMap<String, Object>();
        messageProperties.put("barValue", "bar");

        MuleMessage message = new DefaultMuleMessage("test event", messageProperties, muleContext);
        assertTrue(router.isMatch(message));

        MuleEvent responseEvent = new OutboundRoutingTestEvent(message, session, muleContext);
        ImmutableEndpoint ep = (ImmutableEndpoint) router.getRoute(2, responseEvent);
        assertEquals("test://foo?bar&exchangePattern=request-response", ep.getEndpointURI().toString());

        when(mockEndpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(responseEvent));
        when(mockEndpoint2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(responseEvent));
        when(mockEndpoint3.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(responseEvent));

        final MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNotNull("This is a sync call, we need a result returned.", result);
        assertEquals(message, result.getMessage());
    }

    @Test
    public void testChainingOutboundRouterAsynchronous() throws Exception
    {
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://test");
        assertNotNull(endpoint1);
        OutboundEndpoint mep1 = RouterTestUtils.createMockEndpoint(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", "test://test");
        assertNotNull(endpoint2);
        OutboundEndpoint mep2 = RouterTestUtils.createMockEndpoint(endpoint2);

        endpoints.clear();
        endpoints.add(mep1);
        endpoints.add(mep2);
        router.setRoutes(new ArrayList<MessageProcessor>(endpoints));

        MuleMessage message = new DefaultMuleMessage("test event", muleContext);
        assertTrue(router.isMatch(message));

        message = new DefaultMuleMessage("test event", muleContext);

        MuleEvent event = new OutboundRoutingTestEvent(message, session, muleContext);

        when(mep1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));
        when(mep2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());

        final MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNull("Async call shouldn't return any result.", result);
    }

    /**
     * One of the targets returns null and breaks the chain
     */
    @Test
    public void testBrokenChain() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test event", muleContext);

        when(mockEndpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());

        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNull(result);
    }
}
