/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.service.Service;
import org.mule.routing.EventCorrelatorCallback;
import org.mule.routing.ResequenceMessagesCorrelatorCallback;
import org.mule.routing.inbound.CorrelationSequenceComparator;
import org.mule.routing.inbound.EventGroup;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.Comparator;
import java.util.List;

public class CorrelationEventResequencingMessageProcessorTestCase extends AbstractMuleTestCase
{

    public CorrelationEventResequencingMessageProcessorTestCase()
    {
        setStartContext(true);
    }

    public void testMessageResequencer() throws Exception
    {
        MuleSession session = getTestSession(getTestService(), muleContext);
        Service testService = getTestService("test", Apple.class);
        assertNotNull(testService);

        TestEventResequencer router = new TestEventResequencer(3);

        MuleMessage message1 = new DefaultMuleMessage("test event A", muleContext);
        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        MuleMessage message3 = new DefaultMuleMessage("test event C", muleContext);
        message1.setCorrelationId(message1.getUniqueId());
        message2.setCorrelationId(message1.getUniqueId());
        message3.setCorrelationId(message1.getUniqueId());

        ImmutableEndpoint endpoint = getTestOutboundEndpoint(MessageExchangePattern.ONE_WAY);
        MuleEvent event1 = new DefaultMuleEvent(message1, endpoint, session);
        MuleEvent event2 = new DefaultMuleEvent(message2, endpoint, session);
        MuleEvent event3 = new DefaultMuleEvent(message3, endpoint, session);

        assertNull(router.process(event2));
        assertNull(router.process(event3));

        MuleEvent resultEvent = router.process(event1);
        assertNotNull(resultEvent);
        MuleMessage resultMessage = resultEvent.getMessage();
        assertNotNull(resultMessage);
        List results = (List) resultMessage.getPayload();
        assertEquals(3, results.size());

        assertEquals("test event B", results.get(0).toString());
        assertEquals("test event C", results.get(1).toString());
        assertEquals("test event A", results.get(2).toString());

        // set a resequencing comparator. We need to reset the router since it will not process the same event group
        //twice
        router = new TestEventResequencer(3);
        router.setEventComparator(new EventPayloadComparator());

        assertNull(router.process(event2));
        assertNull(router.process(event3));

        resultEvent = router.process(event1);
        assertNotNull(results);
        resultMessage = resultEvent.getMessage();
        assertNotNull(resultMessage);
        results = (List) resultMessage.getPayload();
        assertEquals(3, results.size());

        assertEquals("test event A", results.get(0).toString());
        assertEquals("test event B", results.get(1).toString());
        assertEquals("test event C", results.get(2).toString());
    }

    public static class TestEventResequencer extends CorrelationEventResequencingMessageProcessor
    {
        private int eventCount = 0;
        private int eventthreshold = 1;

        public TestEventResequencer(int eventthreshold)
        {
            super();
            this.eventthreshold = eventthreshold;
            this.setEventComparator(new CorrelationSequenceComparator());
        }

        @Override
        protected EventCorrelatorCallback getCorrelatorCallback(MuleEvent event)
        {
            return new ResequenceMessagesCorrelatorCallback(getEventComparator(), muleContext)
            {
                @Override
                public boolean shouldAggregateEvents(EventGroup events)
                {
                    eventCount++;
                    if (eventCount == eventthreshold)
                    {
                        eventCount = 0;
                        return true;
                    }
                    return false;
                }
            };
        }
    }

    public static class EventPayloadComparator implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            try
            {
                return ((MuleEvent) o1).getMessageAsString().compareTo(((MuleEvent) o2).getMessageAsString());
            }
            catch (MuleException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }

        }
    }
}