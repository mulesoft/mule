/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.service.Service;
import org.mule.routing.correlation.EventCorrelatorCallback;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.Iterator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AggregatorTestCase extends AbstractMuleContextTestCase
{

    public AggregatorTestCase()
    {
        setStartContext(true);
    }

    @Test
    public void testMessageAggregator() throws Exception
    {
        Service testService = getTestService("test", Apple.class);
        MuleSession session = getTestSession(testService, muleContext);

        TestEventAggregator router = new TestEventAggregator(3);
        router.setMuleContext(muleContext);
        router.setFlowConstruct(testService);
        router.initialise();
        MuleMessage message1 = new DefaultMuleMessage("test event A", muleContext);
        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        MuleMessage message3 = new DefaultMuleMessage("test event C", muleContext);
        message1.setCorrelationId(message1.getUniqueId());
        message2.setCorrelationId(message1.getUniqueId());
        message3.setCorrelationId(message1.getUniqueId());

        ImmutableEndpoint endpoint = MuleTestUtils.getTestOutboundEndpoint(MessageExchangePattern.ONE_WAY, muleContext);
        MuleEvent event1 = new DefaultMuleEvent(message1, endpoint, session);
        MuleEvent event2 = new DefaultMuleEvent(message2, endpoint, session);
        MuleEvent event3 = new DefaultMuleEvent(message3, endpoint, session);

        assertNull(router.process(event1));
        assertNull(router.process(event2));

        MuleEvent result = router.process(event3);
        assertNotNull(result);
        assertEquals("test event A test event B test event C ", result.getMessageAsString());
    }

    public static class TestEventAggregator extends AbstractAggregator
    {
        protected final int eventThreshold;
        protected int eventCount = 0;

        public TestEventAggregator(int eventThreshold)
        {
            this.eventThreshold = eventThreshold;
        }

        @Override
        protected EventCorrelatorCallback getCorrelatorCallback(final MuleContext muleContext)
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

                public MuleEvent aggregateEvents(EventGroup events) throws AggregationException
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
                            throw new AggregationException(events, next, e);
                        }
                    }

                    return new DefaultMuleEvent(new DefaultMuleMessage(newPayload.toString(), muleContext),
                        events.getMessageCollectionEvent());
                }
            };
        }
    }
}
