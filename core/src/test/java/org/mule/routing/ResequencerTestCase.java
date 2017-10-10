/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

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
import org.mule.routing.correlation.CorrelationSequenceComparator;
import org.mule.routing.correlation.EventCorrelatorCallback;
import org.mule.routing.correlation.ResequenceMessagesCorrelatorCallback;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.Comparator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResequencerTestCase extends AbstractMuleContextTestCase
{

    @Mock
    private EventGroup eventGroup;
    
    public ResequencerTestCase()
    {
        setStartContext(true);
    }
    
    @Test
    public void returnNullWhenEventGroupHasNoEventToAggregate() throws Exception
    {
        ResequenceMessagesCorrelatorCallback callback = new ResequenceMessagesCorrelatorCallback(new CorrelationSequenceComparator(), muleContext, "dummy");
        when(eventGroup.toArray(false)).thenReturn(new MuleEvent[0]);
        MuleEvent event = callback.aggregateEvents(eventGroup);
        assertThat(event, is(nullValue()));
    }

    @Test
    public void testMessageResequencer() throws Exception
    {
        MuleSession session = getTestSession(null, muleContext);
        Service testService = getTestService("test", Apple.class);
        assertNotNull(testService);

        TestEventResequencer router = new TestEventResequencer(3);
        router.setMuleContext(muleContext);
        router.setFlowConstruct(testService);
        router.initialise();

        MuleMessage message1 = new DefaultMuleMessage("test event A", muleContext);
        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        MuleMessage message3 = new DefaultMuleMessage("test event C", muleContext);
        final String correlationId = message1.getUniqueId();
        message1.setCorrelationId(correlationId);
        message2.setCorrelationId(correlationId);
        message3.setCorrelationId(correlationId);

        InboundEndpoint endpoint = MuleTestUtils.getTestInboundEndpoint(MessageExchangePattern.ONE_WAY,
            muleContext);
        MuleEvent event1 = new DefaultMuleEvent(message1, endpoint, getTestService(), session);
        MuleEvent event2 = new DefaultMuleEvent(message2, endpoint, getTestService(), session);
        MuleEvent event3 = new DefaultMuleEvent(message3, endpoint, getTestService(), session);

        assertNull(router.process(event2));
        assertNull(router.process(event3));

        MuleEvent resultEvent = router.process(event1);
        assertNotNull(resultEvent);
        MuleMessage resultMessage = resultEvent.getMessage();
        assertNotNull(resultMessage);

        assertTrue(resultMessage.getPayloadAsString().equals("test event A")
                   || resultMessage.getPayloadAsString().equals("test event B")
                   || resultMessage.getPayloadAsString().equals("test event C"));

        // set a resequencing comparator. We need to reset the router since it will
        // not process the same event group
        // twice
        router = new TestEventResequencer(3);
        router.setMuleContext(muleContext);
        router.setEventComparator(new EventPayloadComparator());
        testService.setName("testService2");
        router.setFlowConstruct(testService);
        router.initialise();

        assertNull(router.process(event2));
        assertNull(router.process(event3));

        resultEvent = router.process(event1);
        assertNotNull(resultEvent);
        resultMessage = resultEvent.getMessage();
        assertNotNull(resultMessage);

        assertEquals("test event C", resultMessage.getPayloadAsString());
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
