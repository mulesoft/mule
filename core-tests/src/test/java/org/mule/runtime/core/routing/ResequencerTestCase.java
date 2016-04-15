/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.routing.correlation.CorrelationSequenceComparator;
import org.mule.runtime.core.routing.correlation.EventCorrelatorCallback;
import org.mule.runtime.core.routing.correlation.ResequenceMessagesCorrelatorCallback;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.Comparator;

import org.junit.Test;

public class ResequencerTestCase extends AbstractMuleContextTestCase
{

    public ResequencerTestCase()
    {
        setStartContext(true);
    }

    @Test
    public void testMessageResequencer() throws Exception
    {
        MuleSession session = getTestSession(null, muleContext);
        Flow flow = getTestFlow("test", Apple.class);
        assertNotNull(flow);

        TestEventResequencer router = new TestEventResequencer(3);
        router.setMuleContext(muleContext);
        router.setFlowConstruct(flow);
        router.initialise();

        MuleMessage message1 = new DefaultMuleMessage("test event A", muleContext);
        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        MuleMessage message3 = new DefaultMuleMessage("test event C", muleContext);
        final String correlationId = message1.getUniqueId();
        message1.setCorrelationId(correlationId);
        message2.setCorrelationId(correlationId);
        message3.setCorrelationId(correlationId);

        MuleEvent event1 = new DefaultMuleEvent(message1, getTestFlow(), session);
        MuleEvent event2 = new DefaultMuleEvent(message2, getTestFlow(), session);
        MuleEvent event3 = new DefaultMuleEvent(message3, getTestFlow(), session);

        assertNull(router.process(event2));
        assertNull(router.process(event3));

        MuleEvent resultEvent = router.process(event1);
        assertNotNull(resultEvent);
        MuleMessage resultMessage = resultEvent.getMessage();
        assertNotNull(resultMessage);

        assertTrue(getPayloadAsString(resultMessage).equals("test event A")
                   || getPayloadAsString(resultMessage).equals("test event B")
                   || getPayloadAsString(resultMessage).equals("test event C"));

    }

    @Test
    public void testMessageResequencerWithComparator() throws Exception
    {
        MuleSession session = getTestSession(null, muleContext);
        Flow flow = getTestFlow("test", Apple.class);
        assertNotNull(flow);

        TestEventResequencer router = new TestEventResequencer(3);
        router.setMuleContext(muleContext);
        router.setFlowConstruct(flow);
        router.initialise();

        MuleMessage message1 = new DefaultMuleMessage("test event A", muleContext);
        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        MuleMessage message3 = new DefaultMuleMessage("test event C", muleContext);
        final String correlationId = message1.getUniqueId();
        message1.setCorrelationId(correlationId);
        message2.setCorrelationId(correlationId);
        message3.setCorrelationId(correlationId);

        MuleEvent event1 = new DefaultMuleEvent(message1, getTestFlow(), session);
        MuleEvent event2 = new DefaultMuleEvent(message2, getTestFlow(), session);
        MuleEvent event3 = new DefaultMuleEvent(message3, getTestFlow(), session);

        // set a resequencing comparator. We need to reset the router since it will
        // not process the same event group
        // twice
        router = new TestEventResequencer(3);
        router.setMuleContext(muleContext);
        router.setEventComparator(new EventPayloadComparator());
        router.setFlowConstruct(flow);
        router.initialise();

        assertNull(router.process(event2));
        assertNull(router.process(event3));

        MuleEvent resultEvent = router.process(event1);
        assertNotNull(resultEvent);
        MuleMessage resultMessage = resultEvent.getMessage();
        assertNotNull(resultMessage);

        assertEquals("test event C", getPayloadAsString(resultMessage));
    }


    public static class TestEventResequencer extends Resequencer
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
        protected EventCorrelatorCallback getCorrelatorCallback(MuleContext muleContext)
        {
            return new ResequenceMessagesCorrelatorCallback(getEventComparator(), muleContext, storePrefix)
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

        @Override
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
