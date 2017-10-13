/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Collections.singletonMap;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.MuleTestUtils.createAndRegisterFlow;
import static org.mule.tck.MuleTestUtils.createFlow;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.routing.correlation.CorrelationSequenceComparator;
import org.mule.runtime.core.internal.routing.correlation.EventCorrelatorCallback;
import org.mule.runtime.core.internal.routing.correlation.ResequenceMessagesCorrelatorCallback;
import org.mule.runtime.core.privileged.event.DefaultMuleSession;
import org.mule.runtime.core.privileged.event.MuleSession;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.google.common.collect.ImmutableMap;

import java.util.Comparator;
import java.util.Map;

import javax.xml.namespace.QName;

import org.junit.Test;


public class ResequencerTestCase extends AbstractMuleContextTestCase {

  public ResequencerTestCase() {
    setStartContext(true);
  }



  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    return singletonMap(REGISTRY_KEY, componentLocator);
  }

  @Test
  public void testMessageResequencer() throws Exception {
    MuleSession session = new DefaultMuleSession();
    Flow flow = createAndRegisterFlow(muleContext, APPLE_FLOW, componentLocator);
    assertNotNull(flow);

    TestEventResequencer router = new TestEventResequencer(3);
    router.setMuleContext(muleContext);
    router.setAnnotations(getAppleFlowComponentLocationAnnotations());
    initialiseIfNeeded(router, true, muleContext);

    EventContext context = create(flow, TEST_CONNECTOR_LOCATION, "foo");

    Message message1 = Message.of("test event A");
    Message message2 = Message.of("test event B");
    Message message3 = Message.of("test event C");

    CoreEvent event1 = InternalEvent.builder(context).message(message1).session(session).build();
    CoreEvent event2 = InternalEvent.builder(context).message(message2).session(session).build();
    CoreEvent event3 = InternalEvent.builder(context).message(message3).session(session).build();

    assertNull(router.process(event2));
    assertNull(router.process(event3));

    CoreEvent resultEvent = router.process(event1);
    assertNotNull(resultEvent);
    Message resultMessage = resultEvent.getMessage();
    assertNotNull(resultMessage);

    assertTrue(getPayloadAsString(resultMessage).equals("test event A")
        || getPayloadAsString(resultMessage).equals("test event B") || getPayloadAsString(resultMessage).equals("test event C"));

  }

  @Test
  public void testMessageResequencerWithComparator() throws Exception {
    MuleSession session = new DefaultMuleSession();
    Flow flow = createFlow(muleContext, "test");
    assertNotNull(flow);

    when(componentLocator.find(any(Location.class))).thenReturn(of(flow));

    TestEventResequencer router = new TestEventResequencer(3);
    Map<QName, Object> fakeComponentLocationAnnotations =
        ImmutableMap.<QName, Object>builder().put(LOCATION_KEY, fromSingleComponent("test")).build();
    router.setAnnotations(fakeComponentLocationAnnotations);
    initialiseIfNeeded(router, true, muleContext);

    EventContext context = create(flow, TEST_CONNECTOR_LOCATION, "foo");

    Message message1 = Message.of("test event A");
    Message message2 = Message.of("test event B");
    Message message3 = Message.of("test event C");

    CoreEvent event1 =
        InternalEvent.builder(context).message(message1).session(session).build();
    CoreEvent event2 =
        InternalEvent.builder(context).message(message2).session(session).build();
    CoreEvent event3 =
        InternalEvent.builder(context).message(message3).session(session).build();

    // set a resequencing comparator. We need to reset the router since it will
    // not process the same event group
    // twice
    router = new TestEventResequencer(3);
    router.setMuleContext(muleContext);
    router.setEventComparator(new EventPayloadComparator());
    router.setAnnotations(getAppleFlowComponentLocationAnnotations());
    initialiseIfNeeded(router, true, muleContext);

    assertNull(router.process(event2));
    assertNull(router.process(event3));

    CoreEvent resultEvent = router.process(event1);
    assertNotNull(resultEvent);
    Message resultMessage = resultEvent.getMessage();
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
        return ((PrivilegedEvent) o1).getMessageAsString(muleContext)
            .compareTo(((PrivilegedEvent) o2).getMessageAsString(muleContext));
      } catch (MuleException e) {
        throw new IllegalArgumentException(e.getMessage());
      }

    }
  }
}
