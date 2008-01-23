/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.inbound;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.component.Component;
import org.mule.api.endpoint.Endpoint;
import org.mule.api.routing.InboundRouterCollection;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.testmodels.fruit.Apple;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

public class IdempotentReceiverTestCase extends AbstractMuleTestCase
{

    public void testIdempotentReceiver() throws Exception
    {
        IdempotentReceiver router = new IdempotentReceiver();

        Mock session = MuleTestUtils.getMockSession();
        Component testComponent = getTestComponent("test", Apple.class);

        InboundRouterCollection messageRouter = new DefaultInboundRouterCollection();

        messageRouter.addRouter(router);
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        MuleMessage message = new DefaultMuleMessage("test event");

        Endpoint endpoint = getTestEndpoint("Test1Provider", Endpoint.ENDPOINT_TYPE_SENDER);
        MuleEvent event = new DefaultMuleEvent(message, endpoint, (MuleSession) session.proxy(), false);
        // called by idempotent receiver as this is the fist event it will try
        // and initialize the id store
        session.expectAndReturn("getComponent", testComponent);

        assertTrue(router.isMatch(event));

        session.expect("dispatchEvent", C.eq(event));
        // called by Inbound message router
        session.expectAndReturn("getComponent", testComponent);

        // called by idempotent receiver
        session.expectAndReturn("getComponent", testComponent);
        messageRouter.route(event);

        session.verify();
        message = new DefaultMuleMessage("test event");
        event = new DefaultMuleEvent(message, endpoint, (MuleSession) session.proxy(), true);

        session.expectAndReturn("sendEvent", C.eq(event), message);
        // called by idempotent receiver
        session.expectAndReturn("getComponent", testComponent);
        // called by Inbound message router
        session.expectAndReturn("getComponent", testComponent);
        MuleMessage result = messageRouter.route(event);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();

        session.expect("toString");
        // called by idempotent receiver
        session.expectAndReturn("getComponent", testComponent);

        event = new DefaultMuleEvent(message, endpoint, (MuleSession) session.proxy(), false);
        // we've already received this message
        assertTrue(!router.isMatch(event));

        messageRouter.route(event);
        session.verify();
    }

}
