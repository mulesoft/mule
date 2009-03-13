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
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.routing.InboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transformer.simple.ObjectToByteArray;
import org.mule.transformer.simple.StringAppendTransformer;
import org.mule.util.CollectionUtils;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;

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

        ImmutableEndpoint endpoint = getTestOutboundEndpoint("Test1Provider");
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

        ImmutableEndpoint endpoint = getTestOutboundEndpoint("Test1Provider", CollectionUtils.singletonList(new ObjectToByteArray()));
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
    
    public void testConsecutiveSelectiveConsumersWithTransformFirst() throws Exception
    {
        Service testService = getTestService("test", Apple.class);

        InboundRouterCollection messageRouter = new DefaultInboundRouterCollection();
        SelectiveConsumer router = new TestSelectiveConsumer("test");
        router.setFilter(new PayloadTypeFilter(String.class));
        router.setTransformFirst(true);
        messageRouter.addRouter(router);
        
        // NOTE: The second router is invoked with the same message instance as the
        // preceding router and has already been transformed (transformFirst was set
        // on previous router).  See comments on MULE-4240.
        SelectiveConsumer router2 = new TestSelectiveConsumer("test TRANSFORMED");
        messageRouter.addRouter(router2);

        testService.setInboundRouter(messageRouter);

        testService.start();
        List<Transformer> transformers = new ArrayList<Transformer>();
        transformers.add(new StringAppendTransformer(" TRANSFORMED"));

        messageRouter.route(getTestEvent("test", getTestInboundEndpoint("endpoint", transformers)));

    }

    private static class TestSelectiveConsumer extends SelectiveConsumer
    {
        private String expect;

        public TestSelectiveConsumer(String expect)
        {
            this.expect = expect;
        }

        @Override
        public boolean isMatch(MuleEvent event) throws MessagingException
        {
            assertEquals(expect, event.getMessage().getPayload());
            super.isMatch(event);
            return false;
        }
    }

}
