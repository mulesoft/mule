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
import org.mule.components.simple.EchoComponent;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.RegExFilter;
import org.mule.routing.outbound.ExceptionBasedRouter;
import org.mule.routing.outbound.OutboundMessageRouter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ExceptionBasedRouterTestCase extends AbstractMuleTestCase
{
    public void testSuccessfulExceptionRouter() throws Exception
    {
        Mock session = getMockSession();
        OutboundMessageRouter messageRouter = new OutboundMessageRouter();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        UMOEndpoint endpoint1 = new MuleEndpoint("test://AlwaysFail", false);
        UMOEndpoint endpoint2 = new MuleEndpoint("test://AlwaysFail", false);

        UMOEndpoint endpoint3 = new MuleEndpoint("test://Dummy", false);

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        RegExFilter filter = new RegExFilter("(.*) event");
        router.setFilter(filter);
        List endpoints = new ArrayList();
        endpoints.add(endpoint1);
        endpoints.add(endpoint2);
        endpoints.add(endpoint3);
        router.setEndpoints(endpoints);

        assertEquals(filter, router.getFilter());

        UMOMessage message = new MuleMessage("test event", null);

        assertTrue(router.isMatch(message));

        //Only one dispatch should be called as the others should fail
        session.expect("dispatchEvent", C.eq(message, endpoint1));
        router.route(message, (UMOSession) session.proxy(), false);
        session.verify();

        message = new MuleMessage("test event", null);

        //Only one send should be called as the others should fail
        session.expectAndReturn("sendEvent", C.eq(message, endpoint1), message);
        UMOMessage result = router.route(message, (UMOSession) session.proxy(), true);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();
    }

    public void testFailingRouterRouter() throws Exception
    {
        UMOSession session = getTestSession(getTestComponent(getTestDescriptor("test", EchoComponent.class.getName())));
        OutboundMessageRouter messageRouter = new OutboundMessageRouter();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        UMOEndpoint endpoint1 = new MuleEndpoint("test://AlwaysFail", false);
        UMOEndpoint endpoint2 = new MuleEndpoint("test://AlwaysFail", false);

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        RegExFilter filter = new RegExFilter("(.*) event");
        router.setFilter(filter);
        List endpoints = new ArrayList();
        endpoints.add(endpoint1);
        endpoints.add(endpoint2);
        router.setEndpoints(endpoints);

        assertEquals(filter, router.getFilter());

        UMOMessage message = new MuleMessage("test event", null);

        assertTrue(router.isMatch(message));

        try {
            router.route(message, session, false);
            fail("Should have thrown exception as both endpoints would have failed");
        } catch (CouldNotRouteOutboundMessageException e) {
            //expected
        }
        message = new MuleMessage("test event", null);

        UMOMessage result = null;
        try {
            result = router.route(message, session, true);
            fail("Should have thrown exception as both endpoints would have failed");
        } catch (CouldNotRouteOutboundMessageException e) {
            //expected
        }
        assertNull(result);
    }

    public void testSuccessfulExceptionRouterWithNoException() throws Exception
    {
        Mock session = getMockSession();
        OutboundMessageRouter messageRouter = new OutboundMessageRouter();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        UMOEndpoint endpoint1 = new MuleEndpoint("test://Dummy1", false);
        UMOEndpoint endpoint2 = new MuleEndpoint("test://Dummy2", false);

        ExceptionBasedRouter router = new ExceptionBasedRouter();
        RegExFilter filter = new RegExFilter("(.*) event");
        router.setFilter(filter);
        List endpoints = new ArrayList();
        endpoints.add(endpoint1);
        endpoints.add(endpoint2);
        router.setEndpoints(endpoints);

        assertEquals(filter, router.getFilter());

        UMOMessage message = new MuleMessage("test event", null);

        assertTrue(router.isMatch(message));

        //Only one dispatch should be called as the others should fail
        session.expect("dispatchEvent", C.eq(message, endpoint1));
        router.route(message, (UMOSession) session.proxy(), false);
        session.verify();

        message = new MuleMessage("test event", null);

        //Only one send should be called as the others should fail
        session.expectAndReturn("sendEvent", C.eq(message, endpoint1), message);
        UMOMessage result = router.route(message, (UMOSession) session.proxy(), true);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();
    }
}
