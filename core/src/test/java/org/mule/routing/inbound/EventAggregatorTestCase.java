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
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.routing.InboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.routing.AggregationException;
import org.mule.routing.EventCorrelatorCallback;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.Iterator;
import java.util.Map;

public class EventAggregatorTestCase extends AbstractMuleTestCase
{

    public void testMessageAggregator() throws Exception
    {
        Service testService = getTestService("test", Apple.class);
        MuleSession session = getTestSession(testService, muleContext);

        InboundRouterCollection messageRouter = new DefaultInboundRouterCollection();
        TestEventAggregator router = new TestEventAggregator(3);
        messageRouter.addRouter(router);
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());
        router.setMuleContext(muleContext);
        router.initialise();

        MuleMessage message1 = new DefaultMuleMessage("test event A");
        MuleMessage message2 = new DefaultMuleMessage("test event B");
        MuleMessage message3 = new DefaultMuleMessage("test event C");
        message1.setCorrelationId(message1.getUniqueId());
        message2.setCorrelationId(message1.getUniqueId());
        message3.setCorrelationId(message1.getUniqueId());

        ImmutableEndpoint endpoint = getTestOutboundEndpoint("Test1Provider");
        MuleEvent event1 = new DefaultMuleEvent(message1, endpoint, session, false);
        MuleEvent event2 = new DefaultMuleEvent(message2, endpoint, session, false);
        MuleEvent event3 = new DefaultMuleEvent(message3, endpoint, session, false);
        assertTrue(router.isMatch(event1));
        assertTrue(router.isMatch(event2));
        assertTrue(router.isMatch(event3));

        assertNull(router.process(event1));
        assertNull(router.process(event2));

        MuleEvent[] results = router.process(event3);
        assertNotNull(results);
        assertEquals(1, results.length);
        assertEquals("test event A test event B test event C ", results[0].getMessageAsString());
    }

    public static class TestEventAggregator extends AbstractEventAggregator
    {
        protected final int eventThreshold;
        protected int eventCount = 0;

        public TestEventAggregator(int eventThreshold)
        {
            this.eventThreshold = eventThreshold;
        }

        protected EventCorrelatorCallback getCorrelatorCallback()
        {
            return new EventCorrelatorCallback()
            {
                public boolean shouldAggregateEvents(EventGroup events)
                {
                    eventCount++;
                    if (eventCount == eventThreshold)
                    {
                        eventCount = 0;
                        return true;
                    }
                    return false;
                }

                public EventGroup createEventGroup(MuleEvent event, Object groupId)
                {
                    return new EventGroup(groupId, eventThreshold);
                }

                public MuleMessage aggregateEvents(EventGroup events) throws AggregationException
                {
                    if (events.size() != eventThreshold)
                    {
                        throw new IllegalStateException("eventThreshold not yet reached?");
                    }

                    StringBuffer newPayload = new StringBuffer(80);

                    for (Iterator iterator = events.iterator(); iterator.hasNext();)
                    {
                        MuleEvent event = (MuleEvent) iterator.next();
                        try
                        {
                            newPayload.append(event.getMessageAsString()).append(" ");
                        }
                        catch (MuleException e)
                        {
                            throw new AggregationException(events, event.getEndpoint(), e);
                        }
                    }

                    return new DefaultMuleMessage(newPayload.toString(), (Map) null);
                }
            };
        }
    }
}
