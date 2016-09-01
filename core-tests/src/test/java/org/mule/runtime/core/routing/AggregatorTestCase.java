/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static java.util.Optional.of;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MessageContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.routing.correlation.EventCorrelatorCallback;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.Iterator;

import org.junit.Test;


public class AggregatorTestCase extends AbstractMuleContextTestCase {

  public AggregatorTestCase() {
    setStartContext(true);
  }

  @Test
  public void testMessageAggregator() throws Exception {
    Flow flow = getTestFlow("test", Apple.class);
    MuleSession session = getTestSession(flow, muleContext);

    TestEventAggregator router = new TestEventAggregator(3);
    router.setMuleContext(muleContext);
    router.setFlowConstruct(flow);
    router.initialise();

    MessageContext context = DefaultMessageContext.create(flow, TEST_CONNECTOR, "foo");

    MuleMessage message1 = MuleMessage.builder().payload("test event A").build();
    MuleMessage message2 = MuleMessage.builder().payload("test event B").build();
    MuleMessage message3 = MuleMessage.builder().payload("test event C").build();

    MuleEvent event1 = MuleEvent.builder(context).message(message1).flow(flow).session(session).build();
    MuleEvent event2 = MuleEvent.builder(context).message(message2).flow(flow).session(session).build();
    MuleEvent event3 = MuleEvent.builder(context).message(message3).flow(flow).session(session).build();

    assertNull(router.process(event1));
    assertNull(router.process(event2));

    MuleEvent result = router.process(event3);
    assertNotNull(result);
    assertTrue(result.getMessageAsString(muleContext).contains("test event A"));
    assertTrue(result.getMessageAsString(muleContext).contains("test event B"));
    assertTrue(result.getMessageAsString(muleContext).contains("test event C"));
    assertTrue(result.getMessageAsString(muleContext).matches("test event [A,B,C] test event [A,B,C] test event [A,B,C] "));
  }

  public static class TestEventAggregator extends AbstractAggregator {

    protected final int eventThreshold;
    protected int eventCount = 0;

    public TestEventAggregator(int eventThreshold) {
      this.eventThreshold = eventThreshold;
    }

    @Override
    protected EventCorrelatorCallback getCorrelatorCallback(final MuleContext muleContext) {
      return new EventCorrelatorCallback() {

        @Override
        public boolean shouldAggregateEvents(EventGroup events) {
          eventCount++;
          if (eventCount == eventThreshold) {
            eventCount = 0;
            return true;
          }
          return false;
        }

        @Override
        public EventGroup createEventGroup(MuleEvent event, Object groupId) {
          return new EventGroup(groupId, muleContext, of(eventThreshold), storePrefix);
        }

        @Override
        public MuleEvent aggregateEvents(EventGroup events) throws AggregationException {
          if (events.size() != eventThreshold) {
            throw new IllegalStateException("eventThreshold not yet reached?");
          }

          StringBuilder newPayload = new StringBuilder(80);

          try {
            for (Iterator iterator = events.iterator(false); iterator.hasNext();) {
              MuleEvent event = (MuleEvent) iterator.next();
              try {
                newPayload.append(event.getMessageAsString(muleContext)).append(" ");
              } catch (MuleException e) {
                throw new AggregationException(events, next, e);
              }
            }
          } catch (ObjectStoreException e) {
            throw new AggregationException(events, next, e);
          }

          return MuleEvent.builder(events.getMessageCollectionEvent())
              .message(MuleMessage.builder().payload(newPayload.toString()).build()).build();
        }
      };
    }
  }
}
