/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.store.ObjectStoreException;
import org.mule.routing.correlation.EventCorrelatorCallback;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.Iterator;

import org.junit.Test;

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

        InboundEndpoint endpoint = MuleTestUtils.getTestInboundEndpoint(MessageExchangePattern.ONE_WAY, muleContext);
        MuleEvent event1 = new DefaultMuleEvent(message1, endpoint, testService, session);
        MuleEvent event2 = new DefaultMuleEvent(message2, endpoint, testService, session);
        MuleEvent event3 = new DefaultMuleEvent(message3, endpoint, testService, session);

        assertNull(router.process(event1));
        assertNull(router.process(event2));

        MuleEvent result = router.process(event3);
        assertNotNull(result);
        assertTrue(result.getMessageAsString().contains("test event A"));
        assertTrue(result.getMessageAsString().contains("test event B"));
        assertTrue(result.getMessageAsString().contains("test event C"));
        assertTrue(result.getMessageAsString().matches("test event [A,B,C] test event [A,B,C] test event [A,B,C] "));
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
                    return new EventGroup(groupId,muleContext, eventThreshold,false,this.getClass().getName());
                }

                public MuleEvent aggregateEvents(EventGroup events) throws AggregationException
                {
                    if (events.size() != eventThreshold)
                    {
                        throw new IllegalStateException("eventThreshold not yet reached?");
                    }

                    StringBuilder newPayload = new StringBuilder(80);

                    try
                    {
                        for (Iterator iterator = events.iterator(false); iterator.hasNext();)
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
                    }
                    catch (ObjectStoreException e)
                    {
                        throw new AggregationException(events, next, e); 
                    }

                    return new DefaultMuleEvent(new DefaultMuleMessage(newPayload.toString(), muleContext),
                        events.getMessageCollectionEvent());
                }
            };
        }
    }
}
