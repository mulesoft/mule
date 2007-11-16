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

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transformers.simple.ObjectToByteArray;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.util.CollectionUtils;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

public class SelectiveConsumerTestCase extends AbstractMuleTestCase
{

    public void testSelectiveConsumer() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        UMOComponent testComponent = getTestComponent("test", Apple.class);

        UMOInboundRouterCollection messageRouter = new InboundRouterCollection();
        SelectiveConsumer router = new SelectiveConsumer();
        messageRouter.addRouter(router);
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        PayloadTypeFilter filter = new PayloadTypeFilter(String.class);
        router.setFilter(filter);

        assertEquals(filter, router.getFilter());
        UMOMessage message = new MuleMessage("test event");

        UMOEndpoint endpoint = getTestEndpoint("Test1Provider", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        UMOEvent event = new MuleEvent(message, endpoint, (UMOSession) session.proxy(), false);
        assertTrue(router.isMatch(event));

        session.expect("dispatchEvent", C.eq(event));
        session.expectAndReturn("getComponent", testComponent);
        messageRouter.route(event);
        session.verify();

        event = new MuleEvent(message, endpoint, (UMOSession) session.proxy(), true);

        session.expectAndReturn("sendEvent", C.eq(event), message);
        session.expectAndReturn("getComponent", testComponent);
        UMOMessage result = messageRouter.route(event);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();

        session.expectAndReturn("getComponent", testComponent);
        session.expectAndReturn("toString", "");
        message = new MuleMessage(new Exception());

        event = new MuleEvent(message, endpoint, (UMOSession) session.proxy(), false);
        assertTrue(!router.isMatch(event));

        messageRouter.route(event);
        session.verify();
    }

    public void testSelectiveConsumerWithTransformer() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        UMOComponent testComponent = getTestComponent("test", Apple.class);

        UMOInboundRouterCollection messageRouter = new InboundRouterCollection();
        SelectiveConsumer router = new SelectiveConsumer();
        messageRouter.addRouter(router);
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        PayloadTypeFilter filter = new PayloadTypeFilter(byte[].class);
        router.setFilter(filter);

        assertEquals(filter, router.getFilter());
        UMOMessage message = new MuleMessage("test event");

        UMOEndpoint endpoint = getTestEndpoint("Test1Provider", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        endpoint.setTransformers(CollectionUtils.singletonList(new ObjectToByteArray()));
        UMOEvent event = new MuleEvent(message, endpoint, (UMOSession) session.proxy(), false);
        assertTrue(router.isMatch(event));

        session.expect("dispatchEvent", C.eq(event));
        session.expectAndReturn("getComponent", testComponent);
        messageRouter.route(event);
        session.verify();

        event = new MuleEvent(message, endpoint, (UMOSession) session.proxy(), true);

        session.expectAndReturn("sendEvent", C.eq(event), message);
        session.expectAndReturn("getComponent", testComponent);
        UMOMessage result = messageRouter.route(event);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();

        session.expectAndReturn("getComponent", testComponent);
        session.expectAndReturn("toString", "");
        message = new MuleMessage("Hello String");

        event = new MuleEvent(message, endpoint, (UMOSession) session.proxy(), false);
        router.setTransformFirst(false);
        assertTrue(!router.isMatch(event));

        messageRouter.route(event);
        session.verify();

    }
}
