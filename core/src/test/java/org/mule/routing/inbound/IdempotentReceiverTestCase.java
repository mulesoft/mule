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
import org.mule.api.store.ObjectStore;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.testmodels.fruit.Apple;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.Map;

import org.apache.commons.collections.map.HashedMap;

public class IdempotentReceiverTestCase extends AbstractMuleTestCase
{

    public void testIdempotentReceiverDefaultInMemoryObjectStore() throws Exception
    {
        IdempotentReceiver router = new IdempotentReceiver();
        router.setMuleContext(muleContext);

        testIdempotentRouter(router);
    }

    public void testIdempotentReceiverCustomIDStore() throws Exception
    {
        IdempotentReceiver router = new IdempotentReceiver();
        router.setStore(new ObjectStore()
        {
            private Map store = new HashedMap();

            public boolean storeObject(String id, Object item) throws Exception
            {
                store.put(id, item);
                return true;
            }

            public Object retrieveObject(String id) throws Exception
            {
                return store.get(id);
            }

            public boolean removeObject(String id) throws Exception
            {
                store.remove(id);
                return true;
            }

            public boolean containsObject(String id) throws Exception
            {
                return store.containsKey(id);
            }
        });

        router.initialise();

        testIdempotentRouter(router);
    }

    protected void testIdempotentRouter(IdempotentReceiver router) throws Exception, MessagingException
    {
        Mock session = MuleTestUtils.getMockSession();
        Service testService = getTestService("test", Apple.class);

        InboundRouterCollection messageRouter = new DefaultInboundRouterCollection();

        messageRouter.addRouter(router);
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        MuleMessage message = new DefaultMuleMessage("test event");

        ImmutableEndpoint endpoint = getTestOutboundEndpoint("Test1Provider");
        MuleEvent event = new DefaultMuleEvent(message, endpoint, (MuleSession) session.proxy(), false);
        // called by idempotent receiver as this is the fist event it will try
        // and initialize the id store
        session.expectAndReturn("getService", testService);

        assertTrue(router.isMatch(event));

        session.expect("dispatchEvent", C.eq(event));
        // called by Inbound message router
        session.expectAndReturn("getService", testService);

        // called by idempotent receiver
        session.expectAndReturn("getService", testService);
        messageRouter.route(event);

        session.verify();
        message = new DefaultMuleMessage("test event");
        event = new DefaultMuleEvent(message, endpoint, (MuleSession) session.proxy(), true);

        session.expectAndReturn("sendEvent", C.eq(event), message);
        // called by idempotent receiver
        session.expectAndReturn("getService", testService);
        // called by Inbound message router
        session.expectAndReturn("getService", testService);
        MuleMessage result = messageRouter.route(event);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();

        session.expect("toString");
        // called by idempotent receiver
        session.expectAndReturn("getService", testService);

        event = new DefaultMuleEvent(message, endpoint, (MuleSession) session.proxy(), false);
        // we've already received this message
        assertTrue(!router.isMatch(event));

        messageRouter.route(event);
        session.verify();
    }

}
