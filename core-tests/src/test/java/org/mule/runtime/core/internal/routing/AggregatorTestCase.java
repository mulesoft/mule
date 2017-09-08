/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Optional.of;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.api.event.BaseEventContext.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.event.BaseEventContext;
import org.mule.runtime.core.api.event.MuleSession;
import org.mule.runtime.core.api.session.DefaultMuleSession;
import org.mule.runtime.core.internal.routing.correlation.EventCorrelatorCallback;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import java.util.Iterator;


public class AggregatorTestCase extends AbstractMuleContextTestCase {

  public AggregatorTestCase() {
    setStartContext(true);
  }

  @Test
  public void testMessageAggregator() throws Exception {
    Flow flow = getNamedTestFlow("test");
    MuleSession session = new DefaultMuleSession();

    TestEventAggregator router = new TestEventAggregator(3);
    router.setAnnotations(getAppleFlowComponentLocationAnnotations());
    initialiseIfNeeded(router, true, muleContext);

    BaseEventContext context = create(flow, TEST_CONNECTOR_LOCATION, "foo");

    Message message1 = Message.of("test event A");
    Message message2 = Message.of("test event B");
    Message message3 = Message.of("test event C");

    BaseEvent event1 = BaseEvent.builder(context).message(message1).flow(flow).session(session).build();
    BaseEvent event2 = BaseEvent.builder(context).message(message2).flow(flow).session(session).build();
    BaseEvent event3 = BaseEvent.builder(context).message(message3).flow(flow).session(session).build();

    assertNull(router.process(event1));
    assertNull(router.process(event2));

    BaseEvent result = router.process(event3);
    assertNotNull(result);
    PrivilegedEvent privilegedResult = (PrivilegedEvent) result;
    assertTrue(privilegedResult.getMessageAsString(muleContext).contains("test event A"));
    assertTrue(privilegedResult.getMessageAsString(muleContext).contains("test event B"));
    assertTrue(privilegedResult.getMessageAsString(muleContext).contains("test event C"));
    assertTrue(privilegedResult.getMessageAsString(muleContext)
        .matches("test event [A,B,C] test event [A,B,C] test event [A,B,C] "));
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
        public EventGroup createEventGroup(BaseEvent event, Object groupId) {
          return new EventGroup(groupId, muleContext, of(eventThreshold), storePrefix);
        }

        @Override
        public BaseEvent aggregateEvents(EventGroup events) throws AggregationException {
          if (events.size() != eventThreshold) {
            throw new IllegalStateException("eventThreshold not yet reached?");
          }

          StringBuilder newPayload = new StringBuilder(80);

          try {
            for (Iterator iterator = events.iterator(false); iterator.hasNext();) {
              BaseEvent event = (BaseEvent) iterator.next();
              try {
                newPayload.append(((PrivilegedEvent) event).getMessageAsString(muleContext)).append(" ");
              } catch (MuleException e) {
                throw new AggregationException(events, next, e);
              }
            }
          } catch (ObjectStoreException e) {
            throw new AggregationException(events, next, e);
          }

          return BaseEvent.builder(events.getMessageCollectionEvent()).message(Message.of(newPayload.toString())).build();
        }
      };
    }
  }
}
