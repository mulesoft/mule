/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Collections.singletonMap;
import static java.util.OptionalInt.of;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.meta.AbstractAnnotatedObject.LOCATION_KEY;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.junit.Test;
import spire.util.Opt;

public class SimpleCollectionAggregatorTestCase extends AbstractMuleContextTestCase {

  public SimpleCollectionAggregatorTestCase() {
    setStartContext(true);
  }

  @Test
  public void testAggregateMultipleEvents() throws Exception {

    Flow flow = getNamedTestFlow("test");
    assertNotNull(flow);

    SimpleCollectionAggregator router = new SimpleCollectionAggregator();
    SensingNullMessageProcessor sensingMessageProcessor = getSensingNullMessageProcessor();
    router.setListener(sensingMessageProcessor);
    router.setMuleContext(muleContext);
    router.setFlowConstruct(flow);
    router.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
    router.initialise();

    EventContext executionContext = DefaultEventContext.create(flow, TEST_CONNECTOR_LOCATION, "foo");

    Message message1 = Message.of("test event A");
    Message message2 = Message.of("test event B");
    Message message3 = Message.of("test event C");

    Event event1 =
        Event.builder(executionContext).message(message1).groupCorrelation(Optional.of(GroupCorrelation.of(0, 3))).flow(flow)
            .build();
    Event event2 = Event.builder(executionContext).message(message2).flow(flow).build();
    Event event3 = Event.builder(executionContext).message(message3).flow(flow).build();

    assertNull(router.process(event1));
    assertNull(router.process(event2));

    Event resultEvent = router.process(event3);

    assertNotNull(sensingMessageProcessor.event);
    assertThat(resultEvent, equalTo(sensingMessageProcessor.event));

    Message nextMessage = sensingMessageProcessor.event.getMessage();
    assertNotNull(nextMessage);
    assertTrue(nextMessage.getPayload().getValue() instanceof List<?>);
    List<InternalMessage> list = (List<InternalMessage>) nextMessage.getPayload().getValue();
    assertEquals(3, list.size());
    String[] results = new String[3];
    list.stream().map(msg -> msg.getPayload().getValue()).collect(toList()).toArray(results);
    // Need to sort result because of MULE-5998
    Arrays.sort(results);
    assertEquals("test event A", results[0]);
    assertEquals("test event B", results[1]);
    assertEquals("test event C", results[2]);
  }

  @Test
  public void testAggregateSingleEvent() throws Exception {
    Flow flow = getNamedTestFlow("test");
    assertNotNull(flow);

    SimpleCollectionAggregator router = new SimpleCollectionAggregator();
    SensingNullMessageProcessor sensingMessageProcessor = getSensingNullMessageProcessor();
    router.setListener(sensingMessageProcessor);
    router.setMuleContext(muleContext);
    router.setFlowConstruct(flow);
    router.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
    router.initialise();

    EventContext executionContext = DefaultEventContext.create(flow, TEST_CONNECTOR_LOCATION, "foo");
    Message message1 = of("test event A");

    Event event1 =
        Event.builder(executionContext).message(message1).groupCorrelation(Optional.of(GroupCorrelation.of(0, 1))).flow(flow)
            .build();

    Event resultEvent = router.process(event1);

    assertNotNull(sensingMessageProcessor.event);
    assertThat(resultEvent, equalTo(sensingMessageProcessor.event));

    Message nextMessage = sensingMessageProcessor.event.getMessage();
    assertNotNull(nextMessage);
    assertTrue(nextMessage.getPayload().getValue() instanceof List<?>);
    List<InternalMessage> payload = (List<InternalMessage>) nextMessage.getPayload().getValue();
    assertEquals(1, payload.size());
    assertEquals("test event A", payload.get(0).getPayload().getValue());
  }

  @Test
  public void testAggregateMessageCollections() throws Exception {
    Flow flow = getNamedTestFlow("test");
    assertNotNull(flow);

    SimpleCollectionAggregator router = new SimpleCollectionAggregator();
    router.setMuleContext(muleContext);
    router.setFlowConstruct(flow);
    router.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
    router.initialise();

    EventContext executionContext = DefaultEventContext.create(flow, TEST_CONNECTOR_LOCATION, "foo");

    Message message1 = of("test event A");
    Message message2 = of("test event B");
    Message message3 = of("test event C");
    Message message4 = of("test event D");
    List<Message> list = new ArrayList<>();
    List<Message> list2 = new ArrayList<>();
    list.add(message1);
    list.add(message2);
    list2.add(message3);
    list2.add(message4);
    Message messageCollection1 = Message.of(list);
    Message messageCollection2 = Message.of(list2);

    Event event1 =
        Event.builder(executionContext).message(messageCollection1).groupCorrelation(Optional.of(GroupCorrelation.of(0, 2)))
            .flow(flow)
            .build();
    Event event2 =
        Event.builder(executionContext).message(messageCollection2).groupCorrelation(Optional.of(GroupCorrelation.of(0, 2)))
            .flow(flow)
            .build();

    assertNull(router.process(event1));
    Event resultEvent = router.process(event2);
    assertNotNull(resultEvent);
    Message resultMessage = resultEvent.getMessage();
    assertNotNull(resultMessage);
    List<InternalMessage> payload = (List<InternalMessage>) resultMessage.getPayload().getValue();
    assertEquals(2, payload.size());

    assertEquals("test event A", ((List<InternalMessage>) payload.get(0).getPayload().getValue()).get(0).getPayload().getValue());
    assertEquals("test event B", ((List<InternalMessage>) payload.get(0).getPayload().getValue()).get(1).getPayload().getValue());
    assertEquals("test event C", ((List<InternalMessage>) payload.get(1).getPayload().getValue()).get(0).getPayload().getValue());
    assertEquals("test event D", ((List<InternalMessage>) payload.get(1).getPayload().getValue()).get(1).getPayload().getValue());

  }

}
