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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

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
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.mockobjects.dynamic.Mock;

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

        OutboundEndpoint endpoint2 = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint("test://Dummy2");

        OutboundEndpoint endpoint3 = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint("test://Dummy3");

        Mock mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        Mock mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);
        Mock mockendpoint3 = RouterTestUtils.getMockEndpoint(endpoint3);
        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.setMuleContext(muleContext);
        RegExFilter filter = new RegExFilter("(.*) event");
        router.setFilter(filter);
        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add((OutboundEndpoint) mockendpoint1.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint2.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint3.proxy());
        router.setRoutes(endpoints);

        assertEquals(filter, router.getFilter());

        MuleMessage message = new DefaultMuleMessage("test event", muleContext);

        assertTrue(router.isMatch(message));

        mockendpoint1.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());

        MuleSession session = mock(MuleSession.class);
        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNull("Async call should've returned null.", result);
        mockendpoint1.verify();
    }
    */

    @Test
    public void testSuccessfulExceptionRouterSynchronous() throws Exception
    {
        DefaultOutboundRouterCollection messageRouter = new DefaultOutboundRouterCollection();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        OutboundEndpoint endpoint1 =
            muleContext.getEndpointFactory().getOutboundEndpoint("test://Dummy1?exchangePattern=request-response");

        OutboundEndpoint endpoint2 =
            muleContext.getEndpointFactory().getOutboundEndpoint("test://Dummy2?exchangePattern=request-response");

        OutboundEndpoint endpoint3 =
            muleContext.getEndpointFactory().getOutboundEndpoint("test://Dummy3?exchangePattern=request-response");

        Mock mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        Mock mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);
        Mock mockendpoint3 = RouterTestUtils.getMockEndpoint(endpoint3);

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.setMuleContext(muleContext);
        RegExFilter filter = new RegExFilter("(.*) event");
        router.setFilter(filter);
        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add((OutboundEndpoint) mockendpoint1.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint2.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint3.proxy());
        router.setRoutes(endpoints);

        assertEquals(filter, router.getFilter());

        MuleMessage message = new DefaultMuleMessage("test event", muleContext);
        MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);
        // only one send should be called and succeed, the others should not be
        // called
        mockendpoint1.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        MuleSession session = mock(MuleSession.class);
        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNotNull(result);
        assertEquals(message, result.getMessage());
        mockendpoint1.verify();
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

        OutboundEndpoint endpoint2 =
            muleContext.getEndpointFactory().getOutboundEndpoint("test://AlwaysFail");

        Mock mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        Mock mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.setMuleContext(muleContext);

        RegExFilter filter = new RegExFilter("(.*) event");
        router.setFilter(filter);

        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add((OutboundEndpoint) mockendpoint1.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint2.proxy());
        router.setRoutes(endpoints);

        assertEquals(filter, router.getFilter());

        MuleMessage message = new DefaultMuleMessage("test event", muleContext);

        assertTrue(router.isMatch(message));

        // exception to throw
        MuleSession session = mock(MuleSession.class);
        MuleEvent eventToThrow = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, null, session);
        MuleException rex = new RoutingException(eventToThrow, endpoint1);
        mockendpoint1.expectAndThrow("process", RouterTestUtils.getArgListCheckerMuleEvent(), rex);
        mockendpoint2.expectAndThrow("process", RouterTestUtils.getArgListCheckerMuleEvent(), rex);
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
        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("TestSuccessEndpoint",
            "test://Success?exchangePattern=request-response");
        Mock mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        Mock mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.setMuleContext(muleContext);
        router.addRoute((OutboundEndpoint) mockendpoint1.proxy());
        router.addRoute((OutboundEndpoint) mockendpoint2.proxy());

        MuleMessage message = new DefaultMuleMessage("test event", muleContext);
        MuleMessage expectedResultMessage = new DefaultMuleMessage("Return event", muleContext);
        MuleEvent event = new OutboundRoutingTestEvent(expectedResultMessage, null, muleContext);

        assertTrue(router.isMatch(message));

        final MuleSession session = mock(MuleSession.class);
        // exception to throw
        MuleEvent eventToThrow = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, null, session);
        MuleException rex = new RoutingException(eventToThrow, endpoint1);
        // 1st failure
        mockendpoint1.expectAndThrow("process", RouterTestUtils.getArgListCheckerMuleEvent(), rex);
        mockendpoint2.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        MuleEvent actualResult = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        mockendpoint1.verify();
        mockendpoint2.verify();

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
        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("TestSuccessEndpoint",
            "test://Success?exchangePattern=one-way");
        Mock mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        Mock mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.setMuleContext(muleContext);
        router.addRoute((OutboundEndpoint) mockendpoint1.proxy());
        router.addRoute((OutboundEndpoint) mockendpoint2.proxy());

        MuleMessage message = new DefaultMuleMessage("test event", muleContext);
        MuleMessage expectedResultMessage = new DefaultMuleMessage("Return event", muleContext);
        MuleEvent event = new OutboundRoutingTestEvent(expectedResultMessage, null, muleContext);

        assertTrue(router.isMatch(message));

        final MuleSession session = mock(MuleSession.class);
        // exception to throw
        MuleEvent eventToThrow = new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, null, session);
        MuleException rex = new RoutingException(eventToThrow, endpoint1);

        mockendpoint1.expectAndThrow("process", RouterTestUtils.getArgListCheckerMuleEvent(), rex);
        mockendpoint2.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        MuleEvent actualResult = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNull("Async call should not return any results.", actualResult);

        mockendpoint1.verify();
        mockendpoint2.verify();
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
        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("TestSuccessEndpoint",
            "test://Success?exchangePattern=request-response");
        Mock mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        Mock mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);
        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.setMuleContext(muleContext);
        router.addRoute((OutboundEndpoint) mockendpoint1.proxy());
        router.addRoute((OutboundEndpoint) mockendpoint2.proxy());

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
        mockendpoint1.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(),
            exPayloadMessageEvent);
        // next endpoint
        mockendpoint2.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(),
            expectedResultEvent);
        MuleEvent actualResult = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        mockendpoint1.verify();
        mockendpoint2.verify();

        assertEquals("Got an invalid return message.", expectedResultMessage, actualResult.getMessage());
    }
}
