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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutingException;
import org.mule.message.DefaultExceptionPayload;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.RegExFilter;
import org.mule.tck.MuleEventCheckAnswer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ExceptionBasedRouterTestCase extends AbstractMuleContextTestCase
{
    public ExceptionBasedRouterTestCase()
    {
        setStartContext(true);
    }

    /**
     * Multiple targets, no failures. MuleEvent dispatched asynchronously, but forced
     * into sync mode. Test case ends here.
     */
    /* TODO MULE-4476
    @Test
    public void testSuccessfulExceptionRouterAsynchronous() throws Exception
    {
        DefaultOutboundRouterCollection messageRouter = new DefaultOutboundRouterCollection();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        OutboundEndpoint endpoint1 = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint("test://Dummy1");
        OutboundEndpoint mockendpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

        OutboundEndpoint endpoint2 = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint("test://Dummy2");
        OutboundEndpoint mockendpoint2 = RouterTestUtils.createMockEndpoint(endpoint2);

        OutboundEndpoint endpoint3 = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint("test://Dummy3");
        OutboundEndpoint mockendpoint3 = RouterTestUtils.createMockEndpoint(endpoint3);

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.setMuleContext(muleContext);
        RegExFilter filter = new RegExFilter("(.*) event");
        router.setFilter(filter);
        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add(mockendpoint1);
        endpoints.add(mockendpoint2);
        endpoints.add(mockendpoint3);
        router.setRoutes(endpoints);

        assertEquals(filter, router.getFilter());

        MuleMessage message = new DefaultMuleMessage("test event", muleContext);

        assertTrue(router.isMatch(message));

        when(mockendpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());

        MuleSession session = mock(MuleSession.class);
        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNull("Async call should've returned null.", result);
    }
    */

    @Test
    public void testSuccessfulExceptionRouterSynchronous() throws Exception
    {
        DefaultOutboundRouterCollection messageRouter = new DefaultOutboundRouterCollection();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        OutboundEndpoint endpoint1 =
            muleContext.getEndpointFactory().getOutboundEndpoint("test://Dummy1?exchangePattern=request-response");
        OutboundEndpoint mockendpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

        OutboundEndpoint endpoint2 =
            muleContext.getEndpointFactory().getOutboundEndpoint("test://Dummy2?exchangePattern=request-response");
        OutboundEndpoint mockendpoint2 = RouterTestUtils.createMockEndpoint(endpoint2);

        OutboundEndpoint endpoint3 =
            muleContext.getEndpointFactory().getOutboundEndpoint("test://Dummy3?exchangePattern=request-response");
        OutboundEndpoint mockendpoint3 = RouterTestUtils.createMockEndpoint(endpoint3);

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.setMuleContext(muleContext);
        RegExFilter filter = new RegExFilter("(.*) event");
        router.setFilter(filter);
        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add(mockendpoint1);
        endpoints.add(mockendpoint2);
        endpoints.add(mockendpoint3);
        router.setRoutes(endpoints);

        assertEquals(filter, router.getFilter());

        MuleMessage message = new DefaultMuleMessage("test event", muleContext);
        MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);
        // only one send should be called and succeed, the others should not be
        // called
        when(mockendpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));
        MuleSession session = mock(MuleSession.class);
        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNotNull(result);
        assertEquals(message, result.getMessage());
    }

    /**
     * Both targets fail during dispatch. The first endpoint should be forced into
     * sync mode.
     */
    @Test
    public void testBothFailing() throws Exception
    {
        OutboundEndpoint endpoint1 =
            muleContext.getEndpointFactory().getOutboundEndpoint("test://AlwaysFail");
        OutboundEndpoint mockendpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

        OutboundEndpoint endpoint2 =
            muleContext.getEndpointFactory().getOutboundEndpoint("test://AlwaysFail");
        OutboundEndpoint mockendpoint2 = RouterTestUtils.createMockEndpoint(endpoint2);

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.setMuleContext(muleContext);

        RegExFilter filter = new RegExFilter("(.*) event");
        router.setFilter(filter);

        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add(mockendpoint1);
        endpoints.add(mockendpoint2);
        router.setRoutes(endpoints);

        assertEquals(filter, router.getFilter());

        MuleMessage message = new DefaultMuleMessage("test event", muleContext);

        assertTrue(router.isMatch(message));

        // exception to throw
        MuleSession session = mock(MuleSession.class);
        MuleEvent eventToThrow = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, null, session);
        MuleException rex = new RoutingException(eventToThrow, endpoint1);
        when(mockendpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(rex));
        when(mockendpoint2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(rex));
        MuleEvent result = null;
        try
        {
            result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
            fail("Should have thrown exception as both targets would have failed");
        }
        catch (CouldNotRouteOutboundMessageException e)
        {
            // expected
        }
        assertNull("Async call should've returned null.", result);

        message = new DefaultMuleMessage("test event", muleContext);
    }

    /**
     * The first endpoint fails, second succeeds. Events are being sent
     * synchronously.
     */
    @Test
    public void testFailFirstSuccessSecondSync() throws Exception
    {
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("TestFailEndpoint",
            "test://Failure?exchangePattern=request-response");
        OutboundEndpoint mockendpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("TestSuccessEndpoint",
            "test://Success?exchangePattern=request-response");
        OutboundEndpoint mockendpoint2 = RouterTestUtils.createMockEndpoint(endpoint2);

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.setMuleContext(muleContext);
        router.addRoute(mockendpoint1);
        router.addRoute(mockendpoint2);

        MuleMessage message = new DefaultMuleMessage("test event", muleContext);
        MuleMessage expectedResultMessage = new DefaultMuleMessage("Return event", muleContext);
        MuleEvent event = new OutboundRoutingTestEvent(expectedResultMessage, null, muleContext);

        assertTrue(router.isMatch(message));

        final MuleSession session = mock(MuleSession.class);
        // exception to throw
        MuleEvent eventToThrow = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, null, session);
        MuleException rex = new RoutingException(eventToThrow, endpoint1);
        // 1st failure
        when(mockendpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(rex));
        when(mockendpoint2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));

        MuleEvent actualResult = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertEquals("Got an invalid return message.", expectedResultMessage, actualResult.getMessage());
    }

    /**
     * The first endpoint fails, second succeeds. Events are being forced into a sync
     * mode, until we reach the last one.
     */
    @Test
    public void testFailFirstSuccessSecondAsync() throws Exception
    {
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("TestFailEndpoint",
            "test://Failure?exchangePattern=request-response");
        OutboundEndpoint mockendpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("TestSuccessEndpoint",
            "test://Success?exchangePattern=one-way");
        OutboundEndpoint mockendpoint2 = RouterTestUtils.createMockEndpoint(endpoint2);

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.setMuleContext(muleContext);
        router.addRoute(mockendpoint1);
        router.addRoute(mockendpoint2);

        MuleMessage message = new DefaultMuleMessage("test event", muleContext);
        MuleMessage expectedResultMessage = new DefaultMuleMessage("Return event", muleContext);
        MuleEvent event = new OutboundRoutingTestEvent(expectedResultMessage, null, muleContext);

        assertTrue(router.isMatch(message));

        final MuleSession session = mock(MuleSession.class);
        // exception to throw
        MuleEvent eventToThrow = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, null, session);
        MuleException rex = new RoutingException(eventToThrow, endpoint1);

        when(mockendpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(rex));
        when(mockendpoint2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));

        MuleEvent actualResult = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNull("Async call should not return any results.", actualResult);
    }

    /**
     * The first endpoint contains exception payload in return message, second
     * succeeds. Events are being sent synchronously.
     */
    @Test
    public void testFirstHadExceptionPayloadSuccessSecondSyncWithExceptionPayload() throws Exception
    {
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("TestFailEndpoint",
            "test://Failure?exchangePattern=request-response");
        OutboundEndpoint mockendpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("TestSuccessEndpoint",
            "test://Success?exchangePattern=request-response");
        OutboundEndpoint mockendpoint2 = RouterTestUtils.createMockEndpoint(endpoint2);

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.setMuleContext(muleContext);
        router.addRoute(mockendpoint1);
        router.addRoute(mockendpoint2);

        MuleMessage message = new DefaultMuleMessage("test event", muleContext);
        MuleMessage expectedResultMessage = new DefaultMuleMessage("Return event", muleContext);
        MuleEvent expectedResultEvent = new OutboundRoutingTestEvent(expectedResultMessage, null, muleContext);

        assertTrue(router.isMatch(message));

        // remote endpoint failed and set an exception payload on the returned
        // message
        MuleMessage exPayloadMessage = new DefaultMuleMessage("there was a failure", muleContext);
        exPayloadMessage.setExceptionPayload(new DefaultExceptionPayload(new RuntimeException()));
        MuleEvent exPayloadMessageEvent = new OutboundRoutingTestEvent(exPayloadMessage, null, muleContext);


        final MuleSession session = mock(MuleSession.class);
        // 1st failure
        when(mockendpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(exPayloadMessageEvent));
        // next endpoint
        when(mockendpoint2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(expectedResultEvent));

        MuleEvent actualResult = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertEquals("Got an invalid return message.", expectedResultMessage, actualResult.getMessage());
    }
}
