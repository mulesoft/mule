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
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutingException;
import org.mule.message.DefaultExceptionPayload;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.RegExFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;

public class ExceptionBasedRouterTestCase extends AbstractMuleTestCase
{

    /**
     * Multiple endpoints, no failures. MuleEvent dispatched asynchronously, but forced
     * into sync mode. Test case ends here.
     */
    public void testSuccessfulExceptionRouterAsynchronous() throws Exception
    {
        Mock mockSession = MuleTestUtils.getMockSession();
        mockSession.matchAndReturn("getService", getTestService());
        
        DefaultOutboundRouterCollection messageRouter = new DefaultOutboundRouterCollection();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());
 
        ImmutableEndpoint endpoint1 = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint("test://Dummy1");

        ImmutableEndpoint endpoint2 = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint("test://Dummy2");

        ImmutableEndpoint endpoint3 = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint("test://Dummy3");

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        RegExFilter filter = new RegExFilter("(.*) event");
        router.setFilter(filter);
        List endpoints = new ArrayList();
        endpoints.add(endpoint1);
        endpoints.add(endpoint2);
        endpoints.add(endpoint3);
        router.setEndpoints(endpoints);

        assertEquals(filter, router.getFilter());

        MuleMessage message = new DefaultMuleMessage("test event");

        assertTrue(router.isMatch(message));

        mockSession.expect("sendEvent", C.eq(message, endpoint1));
        MuleMessage result = router.route(message, (MuleSession)mockSession.proxy());
        assertNull("Async call should've returned null.", result);
        mockSession.verify();

    }


    public void testSuccessfulExceptionRouterSynchronous() throws Exception
    {
        Mock mockSession = MuleTestUtils.getMockSession();
        mockSession.matchAndReturn("getService", getTestService());

        DefaultOutboundRouterCollection messageRouter = new DefaultOutboundRouterCollection();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        ImmutableEndpoint endpoint1 = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint("test://Dummy1?synchronous=true");

        ImmutableEndpoint endpoint2 = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint("test://Dummy2?synchronous=true");

        ImmutableEndpoint endpoint3 = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint("test://Dummy3?synchronous=true");

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        RegExFilter filter = new RegExFilter("(.*) event");
        router.setFilter(filter);
        List endpoints = new ArrayList();
        endpoints.add(endpoint1);
        endpoints.add(endpoint2);
        endpoints.add(endpoint3);
        router.setEndpoints(endpoints);

        assertEquals(filter, router.getFilter());

        MuleMessage message = new DefaultMuleMessage("test event");

        // only one send should be called and succeed, the others should not be
        // called
        mockSession.expectAndReturn("sendEvent", C.eq(message, endpoint1), message);
        MuleMessage result = router.route(message, (MuleSession)mockSession.proxy());
        assertNotNull(result);
        assertEquals(message, result);
        mockSession.verify();
    }

    /**
     * Both endpoints fail during dispatch. The first endpoint should be forced into
     * sync mode.
     */
    public void testBothFailing() throws Exception
    {
        Mock mockSession = MuleTestUtils.getMockSession();
        mockSession.matchAndReturn("getService", getTestService());

        ImmutableEndpoint endpoint1 = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint("test://AlwaysFail");

        ImmutableEndpoint endpoint2 = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint("test://AlwaysFail");

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        RegExFilter filter = new RegExFilter("(.*) event");
        router.setFilter(filter);
        List endpoints = new ArrayList();
        endpoints.add(endpoint1);
        endpoints.add(endpoint2);
        router.setEndpoints(endpoints);

        assertEquals(filter, router.getFilter());

        MuleMessage message = new DefaultMuleMessage("test event");

        assertTrue(router.isMatch(message));

        // exception to throw
        MuleException rex = new RoutingException(message, endpoint1);
        mockSession.expectAndThrow("sendEvent", C.args(C.eq(message), C.eq(endpoint1)), rex);
        mockSession.expectAndThrow("dispatchEvent", C.args(C.eq(message), C.eq(endpoint2)), rex);
        MuleSession session = (MuleSession)mockSession.proxy();
        MuleMessage result = null;
        try
        {
            result = router.route(message, session);
            fail("Should have thrown exception as both endpoints would have failed");
        }
        catch (CouldNotRouteOutboundMessageException e)
        {
            // expected
        }
        assertNull("Async call should've returned null.", result);
        mockSession.verify();

        message = new DefaultMuleMessage("test event");

    }

    /**
     * The first endpoint fails, second succeeds. Events are being sent
     * synchronously.
     */
    public void testFailFirstSuccessSecondSync() throws Exception
    {
        Mock mockSession = MuleTestUtils.getMockSession();
        mockSession.matchAndReturn("getService", getTestService());

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("TestFailEndpoint", "test://Failure?synchronous=true");
        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("TestSuccessEndpoint", "test://Success?synchronous=true");

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.addEndpoint(endpoint1);
        router.addEndpoint(endpoint2);

        MuleMessage message = new DefaultMuleMessage("test event");
        MuleMessage expectedResultMessage = new DefaultMuleMessage("Return event");

        assertTrue(router.isMatch(message));

        final MuleSession session = (MuleSession)mockSession.proxy();
        // exception to throw
        MuleException rex = new RoutingException(message, endpoint1);
        // 1st failure
        mockSession.expectAndThrow("sendEvent", C.args(C.eq(message), C.eq(endpoint1)), rex);
        // next endpoint
        mockSession.expectAndReturn("sendEvent", C.args(C.eq(message), C.eq(endpoint2)),
            expectedResultMessage);
        MuleMessage actualResultMessage = router.route(message, session);
        mockSession.verify();

        assertEquals("Got an invalid return message.", expectedResultMessage, actualResultMessage);
    }

    /**
     * The first endpoint fails, second succeeds. Events are being forced into a sync
     * mode, until we reach the last one.
     */
    public void testFailFirstSuccessSecondAsync() throws Exception
    {
        Mock mockSession = MuleTestUtils.getMockSession();
        mockSession.matchAndReturn("getService", getTestService());

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("TestFailEndpoint", "test://Failure?synchronous=false");
        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("TestSuccessEndpoint", "test://Success?synchronous=false");

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.addEndpoint(endpoint1);
        router.addEndpoint(endpoint2);

        MuleMessage message = new DefaultMuleMessage("test event");
        MuleMessage expectedResultMessage = new DefaultMuleMessage("Return event");

        assertTrue(router.isMatch(message));

        final MuleSession session = (MuleSession)mockSession.proxy();
        // exception to throw
        MuleException rex = new RoutingException(message, endpoint1);
        // 1st failure
        mockSession.expectAndThrow("sendEvent", C.args(C.eq(message), C.eq(endpoint1)), rex);
        // next endpoint
        mockSession.expectAndReturn("dispatchEvent", C.args(C.eq(message), C.eq(endpoint2)),
            expectedResultMessage);
        MuleMessage actualResultMessage = router.route(message, session);
        assertNull("Async call should not return any results.", actualResultMessage);
        mockSession.verify();
    }

    /**
     * The first endpoint contains exception payload in return message, second
     * succeeds. Events are being sent synchronously.
     */
    public void testFirstHadExceptionPayloadSuccessSecondSyncWithExceptionPayload() throws Exception
    {
        Mock mockSession = MuleTestUtils.getMockSession();
        mockSession.matchAndReturn("getService", getTestService());

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("TestFailEndpoint", "test://Failure?synchronous=true");
        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("TestSuccessEndpoint", "test://Success?synchronous=true");

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.addEndpoint(endpoint1);
        router.addEndpoint(endpoint2);

        MuleMessage message = new DefaultMuleMessage("test event");
        MuleMessage expectedResultMessage = new DefaultMuleMessage("Return event");

        assertTrue(router.isMatch(message));

        // remote endpoint failed and set an exception payload on the returned
        // message
        MuleMessage exPayloadMessage = new DefaultMuleMessage("there was a failure");
        exPayloadMessage.setExceptionPayload(new DefaultExceptionPayload(new RuntimeException()));

        final MuleSession session = (MuleSession)mockSession.proxy();
        // 1st failure
        mockSession.expectAndReturn("sendEvent", C.args(C.eq(message), C.eq(endpoint1)), exPayloadMessage);
        // next endpoint
        mockSession.expectAndReturn("sendEvent", C.args(C.eq(message), C.eq(endpoint2)),
            expectedResultMessage);
        MuleMessage actualResultMessage = router.route(message, session);
        mockSession.verify();

        assertEquals("Got an invalid return message.", expectedResultMessage, actualResultMessage);
    }
}
