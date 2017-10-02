/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.MuleTestUtils.createAndRegisterFlow;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.routing.correlation.EventCorrelatorCallback;
import org.mule.runtime.core.privileged.event.DefaultMuleSession;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import java.util.Iterator;
import java.util.Map;


public class AggregatorTestCase extends AbstractMuleContextTestCase {

  public AggregatorTestCase() {
    setStartContext(true);
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    return singletonMap(REGISTRY_KEY, componentLocator);
  }

  @Test
  public void testMessageAggregator() throws Exception {
    Flow flow = createAndRegisterFlow(muleContext, APPLE_FLOW, componentLocator);
    MuleSession session = new DefaultMuleSession();

    TestEventAggregator router = new TestEventAggregator(3);
    router.setAnnotations(getAppleFlowComponentLocationAnnotations());
    initialiseIfNeeded(router, true, muleContext);

    EventContext context = create(flow, TEST_CONNECTOR_LOCATION, "foo");

    Message message1 = Message.of("test event A");
    Message message2 = Message.of("test event B");
    Message message3 = Message.of("test event C");

    CoreEvent event1 = InternalEvent.builder(context).message(message1).session(session).build();
    CoreEvent event2 = InternalEvent.builder(context).message(message2).session(session).build();
    CoreEvent event3 = InternalEvent.builder(context).message(message3).session(session).build();

    assertNull(router.process(event1));
    assertNull(router.process(event2));

    CoreEvent result = router.process(event3);
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
        public EventGroup createEventGroup(CoreEvent event, Object groupId) {
          return new EventGroup(groupId, muleContext, of(eventThreshold), storePrefix);
        }

        @Override
        public CoreEvent aggregateEvents(EventGroup events) throws AggregationException {
          if (events.size() != eventThreshold) {
            throw new IllegalStateException("eventThreshold not yet reached?");
          }

          StringBuilder newPayload = new StringBuilder(80);

          try {
            for (Iterator iterator = events.iterator(false); iterator.hasNext();) {
              CoreEvent event = (CoreEvent) iterator.next();
              try {
                newPayload.append(((PrivilegedEvent) event).getMessageAsString(muleContext)).append(" ");
              } catch (MuleException e) {
                throw new AggregationException(events, next, e);
              }
            }
          } catch (ObjectStoreException e) {
            throw new AggregationException(events, next, e);
          }

          return CoreEvent.builder(events.getMessageCollectionEvent()).message(Message.of(newPayload.toString())).build();
        }
      };
    }
  }
}
