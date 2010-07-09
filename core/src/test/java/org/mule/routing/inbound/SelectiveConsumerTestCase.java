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
import org.mule.MessageExchangePattern;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.routing.InboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.testmodels.fruit.Apple;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

public class SelectiveConsumerTestCase extends AbstractMuleTestCase
{
    public SelectiveConsumerTestCase()
    {
        setStartContext(true);
    }

    public void testSelectiveConsumer() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        Service testService = getTestService("test", Apple.class);

        InboundRouterCollection messageRouter = createObject(DefaultInboundRouterCollection.class);
        SelectiveConsumer router = createObject(SelectiveConsumer.class);
        messageRouter.addRouter(router);
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        PayloadTypeFilter filter = new PayloadTypeFilter(String.class);
        router.setFilter(filter);

        assertEquals(filter, router.getFilter());
        MuleMessage message = new DefaultMuleMessage("test event", muleContext);

        ImmutableEndpoint endpoint = getTestInboundEndpoint(MessageExchangePattern.ONE_WAY);
        MuleEvent event = new DefaultMuleEvent(message, endpoint, (MuleSession) session.proxy());
        assertTrue(router.isMatch(event));

        session.expect("dispatchEvent", C.eq(event));
        session.expectAndReturn("getService", testService);
        messageRouter.process(event);
        session.verify();

        endpoint = getTestInboundEndpoint(MessageExchangePattern.REQUEST_RESPONSE);
        event = new DefaultMuleEvent(message, endpoint, (MuleSession) session.proxy());

        session.expectAndReturn("sendEvent", C.eq(event), message);
        session.expectAndReturn("getService", testService);
        MuleMessage result = messageRouter.process(event).getMessage();
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();

        session.expectAndReturn("getService", testService);
        session.expectAndReturn("toString", "");
        message = new DefaultMuleMessage(new Exception(), muleContext);

        endpoint = getTestInboundEndpoint(MessageExchangePattern.ONE_WAY);
        event = new DefaultMuleEvent(message, endpoint, (MuleSession) session.proxy());
        assertTrue(!router.isMatch(event));

        messageRouter.process(event);
        session.verify();
    }

    protected static class TestSelectiveConsumer extends SelectiveConsumer
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
