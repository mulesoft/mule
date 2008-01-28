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
import org.mule.api.endpoint.Endpoint;
import org.mule.api.routing.InboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transformer.simple.ObjectToByteArray;
import org.mule.util.CollectionUtils;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

public class SelectiveConsumerTestCase extends AbstractMuleTestCase
{

    public void testSelectiveConsumer() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        Service testService = getTestService("test", Apple.class);

        InboundRouterCollection messageRouter = new DefaultInboundRouterCollection();
        SelectiveConsumer router = new SelectiveConsumer();
        messageRouter.addRouter(router);
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        PayloadTypeFilter filter = new PayloadTypeFilter(String.class);
        router.setFilter(filter);

        assertEquals(filter, router.getFilter());
        MuleMessage message = new DefaultMuleMessage("test event");

        Endpoint endpoint = getTestOutboundEndpoint("Test1Provider");
        MuleEvent event = new DefaultMuleEvent(message, endpoint, (MuleSession) session.proxy(), false);
        assertTrue(router.isMatch(event));

        session.expect("dispatchEvent", C.eq(event));
        session.expectAndReturn("getService", testService);
        messageRouter.route(event);
        session.verify();

        event = new DefaultMuleEvent(message, endpoint, (MuleSession) session.proxy(), true);

        session.expectAndReturn("sendEvent", C.eq(event), message);
        session.expectAndReturn("getService", testService);
        MuleMessage result = messageRouter.route(event);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();

        session.expectAndReturn("getService", testService);
        session.expectAndReturn("toString", "");
        message = new DefaultMuleMessage(new Exception());

        event = new DefaultMuleEvent(message, endpoint, (MuleSession) session.proxy(), false);
        assertTrue(!router.isMatch(event));

        messageRouter.route(event);
        session.verify();
    }

    public void testSelectiveConsumerWithTransformer() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        Service testService = getTestService("test", Apple.class);

        InboundRouterCollection messageRouter = new DefaultInboundRouterCollection();
        SelectiveConsumer router = new SelectiveConsumer();
        messageRouter.addRouter(router);
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        PayloadTypeFilter filter = new PayloadTypeFilter(byte[].class);
        router.setFilter(filter);

        assertEquals(filter, router.getFilter());
        MuleMessage message = new DefaultMuleMessage("test event");

        Endpoint endpoint = getTestOutboundEndpoint("Test1Provider");
        endpoint.setTransformers(CollectionUtils.singletonList(new ObjectToByteArray()));
        MuleEvent event = new DefaultMuleEvent(message, endpoint, (MuleSession) session.proxy(), false);
        assertTrue(router.isMatch(event));

        session.expect("dispatchEvent", C.eq(event));
        session.expectAndReturn("getService", testService);
        messageRouter.route(event);
        session.verify();

        event = new DefaultMuleEvent(message, endpoint, (MuleSession) session.proxy(), true);

        session.expectAndReturn("sendEvent", C.eq(event), message);
        session.expectAndReturn("getService", testService);
        MuleMessage result = messageRouter.route(event);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();

        session.expectAndReturn("getService", testService);
        session.expectAndReturn("toString", "");
        message = new DefaultMuleMessage("Hello String");

        event = new DefaultMuleEvent(message, endpoint, (MuleSession) session.proxy(), false);
        router.setTransformFirst(false);
        assertTrue(!router.isMatch(event));

        messageRouter.route(event);
        session.verify();

    }
}
