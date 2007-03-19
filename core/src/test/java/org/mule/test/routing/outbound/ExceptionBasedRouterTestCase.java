/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing.outbound;

import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.message.ExceptionPayload;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.RegExFilter;
import org.mule.routing.outbound.ExceptionBasedRouter;
import org.mule.routing.outbound.OutboundRouterCollection;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;
import org.mule.umo.routing.RoutingException;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;

public class ExceptionBasedRouterTestCase extends AbstractMuleTestCase
{

    /**
     * Multiple endpoints, no failures. Event dispatched asynchronously, but forced
     * into sync mode. Test case ends here.
     */
    public void testSuccessfulExceptionRouter() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        OutboundRouterCollection messageRouter = new OutboundRouterCollection();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        UMOEndpoint endpoint1 = managementContext.getRegistry().getOrCreateEndpointForUri("test://Dummy1", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        UMOEndpoint endpoint2 = managementContext.getRegistry().getOrCreateEndpointForUri("test://Dummy2", UMOEndpoint.ENDPOINT_TYPE_SENDER);

        UMOEndpoint endpoint3 = managementContext.getRegistry().getOrCreateEndpointForUri("test://Dummy3", UMOEndpoint.ENDPOINT_TYPE_SENDER);

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        RegExFilter filter = new RegExFilter("(.*) event");
        router.setFilter(filter);
        List endpoints = new ArrayList();
        endpoints.add(endpoint1);
        endpoints.add(endpoint2);
        endpoints.add(endpoint3);
        router.setEndpoints(endpoints);

        assertEquals(filter, router.getFilter());

        UMOMessage message = new MuleMessage("test event");

        assertTrue(router.isMatch(message));

        session.expect("sendEvent", C.eq(message, endpoint1));
        UMOMessage result = router.route(message, (UMOSession)session.proxy(), false);
        assertNull("Async call should've returned null.", result);
        session.verify();

        message = new MuleMessage("test event");

        // only one send should be called and succeed, the others should not be
        // called
        session.expectAndReturn("sendEvent", C.eq(message, endpoint1), message);
        result = router.route(message, (UMOSession)session.proxy(), true);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();
    }

    /**
     * Both endpoints fail during dispatch. The first endpoint should be forced into
     * sync mode.
     */
    public void testBothFailing() throws Exception
    {
        Mock mockSession = MuleTestUtils.getMockSession();
        OutboundRouterCollection messageRouter = new OutboundRouterCollection();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        UMOEndpoint endpoint1 = managementContext.getRegistry().getOrCreateEndpointForUri("test://AlwaysFail", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        UMOEndpoint endpoint2 = managementContext.getRegistry().getOrCreateEndpointForUri("test://AlwaysFail", UMOEndpoint.ENDPOINT_TYPE_SENDER);

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        RegExFilter filter = new RegExFilter("(.*) event");
        router.setFilter(filter);
        List endpoints = new ArrayList();
        endpoints.add(endpoint1);
        endpoints.add(endpoint2);
        router.setEndpoints(endpoints);

        assertEquals(filter, router.getFilter());

        UMOMessage message = new MuleMessage("test event");

        assertTrue(router.isMatch(message));

        // exception to throw
        UMOException rex = new RoutingException(message, endpoint1);
        mockSession.expectAndThrow("sendEvent", C.args(C.eq(message), C.eq(endpoint1)), rex);
        mockSession.expectAndThrow("dispatchEvent", C.args(C.eq(message), C.eq(endpoint2)), rex);
        UMOSession session = (UMOSession)mockSession.proxy();
        UMOMessage result = null;
        try
        {
            result = router.route(message, session, false);
            fail("Should have thrown exception as both endpoints would have failed");
        }
        catch (CouldNotRouteOutboundMessageException e)
        {
            // expected
        }
        assertNull("Async call should've returned null.", result);
        mockSession.verify();

        message = new MuleMessage("test event");

    }

    /**
     * The first endpoint fails, second succeeds. Events are being sent
     * synchronously.
     */
    public void testFailFirstSuccessSecondSync() throws Exception
    {
        Mock mockSession = MuleTestUtils.getMockSession();

        UMOEndpoint endpoint1 = getTestEndpoint("TestFailEndpoint", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint1.setEndpointURI(new MuleEndpointURI("test://Failure"));
        UMOEndpoint endpoint2 = getTestEndpoint("TestSuccessEndpoint", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint2.setEndpointURI(new MuleEndpointURI("test://Success"));

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.addEndpoint(endpoint1);
        router.addEndpoint(endpoint2);

        UMOMessage message = new MuleMessage("test event");
        UMOMessage expectedResultMessage = new MuleMessage("Return event");

        assertTrue(router.isMatch(message));

        final UMOSession session = (UMOSession)mockSession.proxy();
        // exception to throw
        UMOException rex = new RoutingException(message, endpoint1);
        // 1st failure
        mockSession.expectAndThrow("sendEvent", C.args(C.eq(message), C.eq(endpoint1)), rex);
        // next endpoint
        mockSession.expectAndReturn("sendEvent", C.args(C.eq(message), C.eq(endpoint2)),
            expectedResultMessage);
        UMOMessage actualResultMessage = router.route(message, session, true);
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

        UMOEndpoint endpoint1 = getTestEndpoint("TestFailEndpoint", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint1.setEndpointURI(new MuleEndpointURI("test://Failure"));
        UMOEndpoint endpoint2 = getTestEndpoint("TestSuccessEndpoint", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint2.setEndpointURI(new MuleEndpointURI("test://Success"));

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.addEndpoint(endpoint1);
        router.addEndpoint(endpoint2);

        UMOMessage message = new MuleMessage("test event");
        UMOMessage expectedResultMessage = new MuleMessage("Return event");

        assertTrue(router.isMatch(message));

        final UMOSession session = (UMOSession)mockSession.proxy();
        // exception to throw
        UMOException rex = new RoutingException(message, endpoint1);
        // 1st failure
        mockSession.expectAndThrow("sendEvent", C.args(C.eq(message), C.eq(endpoint1)), rex);
        // next endpoint
        mockSession.expectAndReturn("dispatchEvent", C.args(C.eq(message), C.eq(endpoint2)),
            expectedResultMessage);
        UMOMessage actualResultMessage = router.route(message, session, false);
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

        UMOEndpoint endpoint1 = getTestEndpoint("TestFailEndpoint", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint1.setEndpointURI(new MuleEndpointURI("test://Failure"));
        UMOEndpoint endpoint2 = getTestEndpoint("TestSuccessEndpoint", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint2.setEndpointURI(new MuleEndpointURI("test://Success"));

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        router.addEndpoint(endpoint1);
        router.addEndpoint(endpoint2);

        UMOMessage message = new MuleMessage("test event");
        UMOMessage expectedResultMessage = new MuleMessage("Return event");

        assertTrue(router.isMatch(message));

        // remote endpoint failed and set an exception payload on the returned
        // message
        UMOMessage exPayloadMessage = new MuleMessage("there was a failure");
        exPayloadMessage.setExceptionPayload(new ExceptionPayload(new RuntimeException()));

        final UMOSession session = (UMOSession)mockSession.proxy();
        // 1st failure
        mockSession.expectAndReturn("sendEvent", C.args(C.eq(message), C.eq(endpoint1)), exPayloadMessage);
        // next endpoint
        mockSession.expectAndReturn("sendEvent", C.args(C.eq(message), C.eq(endpoint2)),
            expectedResultMessage);
        UMOMessage actualResultMessage = router.route(message, session, true);
        mockSession.verify();

        assertEquals("Got an invalid return message.", expectedResultMessage, actualResultMessage);
    }
}
