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

import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.routing.correlation.CorrelationSequenceComparator;
import org.mule.runtime.core.routing.correlation.EventCorrelatorCallback;
import org.mule.runtime.core.routing.correlation.ResequenceMessagesCorrelatorCallback;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.Comparator;

import org.junit.Test;


public class ResequencerTestCase extends AbstractMuleContextTestCase {

  public ResequencerTestCase() {
    setStartContext(true);
  }

  @Test
  public void testMessageResequencer() throws Exception {
    MuleSession session = getTestSession(null, muleContext);
    Flow flow = getTestFlow("test", Apple.class);
    assertNotNull(flow);

    TestEventResequencer router = new TestEventResequencer(3);
    router.setMuleContext(muleContext);
    router.setFlowConstruct(flow);
    router.initialise();

    EventContext context = DefaultEventContext.create(flow, TEST_CONNECTOR, "foo");

    InternalMessage message1 = InternalMessage.builder().payload("test event A").build();
    InternalMessage message2 = InternalMessage.builder().payload("test event B").build();
    InternalMessage message3 = InternalMessage.builder().payload("test event C").build();

    Event event1 = Event.builder(context).message(message1).flow(getTestFlow()).session(session).build();
    Event event2 = Event.builder(context).message(message2).flow(getTestFlow()).session(session).build();
    Event event3 = Event.builder(context).message(message3).flow(getTestFlow()).session(session).build();

    assertNull(router.process(event2));
    assertNull(router.process(event3));

    Event resultEvent = router.process(event1);
    assertNotNull(resultEvent);
    InternalMessage resultMessage = resultEvent.getMessage();
    assertNotNull(resultMessage);

    assertTrue(getPayloadAsString(resultMessage).equals("test event A")
        || getPayloadAsString(resultMessage).equals("test event B") || getPayloadAsString(resultMessage).equals("test event C"));

  }

  @Test
  public void testMessageResequencerWithComparator() throws Exception {
    MuleSession session = getTestSession(null, muleContext);
    Flow flow = getTestFlow("test", Apple.class);
    assertNotNull(flow);

    TestEventResequencer router = new TestEventResequencer(3);
    router.setMuleContext(muleContext);
    router.setFlowConstruct(flow);
    router.initialise();

    EventContext context = DefaultEventContext.create(flow, TEST_CONNECTOR, "foo");

    InternalMessage message1 = InternalMessage.builder().payload("test event A").build();
    InternalMessage message2 = InternalMessage.builder().payload("test event B").build();
    InternalMessage message3 = InternalMessage.builder().payload("test event C").build();

    Event event1 = Event.builder(context).message(message1).flow(getTestFlow()).session(session).build();
    Event event2 = Event.builder(context).message(message2).flow(getTestFlow()).session(session).build();
    Event event3 = Event.builder(context).message(message3).flow(getTestFlow()).session(session).build();

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

    Event resultEvent = router.process(event1);
    assertNotNull(resultEvent);
    InternalMessage resultMessage = resultEvent.getMessage();
    assertNotNull(resultMessage);

    assertEquals("test event C", getPayloadAsString(resultMessage));
  }


  public static class TestEventResequencer extends Resequencer {

    private int eventCount = 0;
    private int eventthreshold = 1;

    public TestEventResequencer(int eventthreshold) {
      super();
      this.eventthreshold = eventthreshold;
      this.setEventComparator(new CorrelationSequenceComparator());
    }

    @Override
    protected EventCorrelatorCallback getCorrelatorCallback(MuleContext muleContext) {
      return new ResequenceMessagesCorrelatorCallback(getEventComparator(), muleContext, storePrefix) {

        @Override
        public boolean shouldAggregateEvents(EventGroup events) {
          eventCount++;
          if (eventCount == eventthreshold) {
            eventCount = 0;
            return true;
          }
          return false;
        }
      };
    }
  }

  public static class EventPayloadComparator implements Comparator {

    @Override
    public int compare(Object o1, Object o2) {
      try {
        return ((Event) o1).getMessageAsString(muleContext).compareTo(((Event) o2).getMessageAsString(muleContext));
      } catch (MuleException e) {
        throw new IllegalArgumentException(e.getMessage());
      }

    }
  }
}
