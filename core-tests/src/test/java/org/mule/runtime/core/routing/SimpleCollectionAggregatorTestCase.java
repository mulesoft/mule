/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.message.GroupCorrelation;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class SimpleCollectionAggregatorTestCase extends AbstractMuleContextTestCase {

  public SimpleCollectionAggregatorTestCase() {
    setStartContext(true);
  }

  @Test
  public void testAggregateMultipleEvents() throws Exception {

    Flow flow = getTestFlowWithComponent("test", Apple.class);
    assertNotNull(flow);

    SimpleCollectionAggregator router = new SimpleCollectionAggregator();
    SensingNullMessageProcessor sensingMessageProcessor = getSensingNullMessageProcessor();
    router.setListener(sensingMessageProcessor);
    router.setMuleContext(muleContext);
    router.setFlowConstruct(flow);
    router.initialise();

    EventContext executionContext = DefaultEventContext.create(flow, TEST_CONNECTOR, "foo");

    InternalMessage message1 = InternalMessage.builder().payload("test event A").build();
    InternalMessage message2 = InternalMessage.builder().payload("test event B").build();
    InternalMessage message3 = InternalMessage.builder().payload("test event C").build();

    Event event1 =
        Event.builder(executionContext).message(message1).groupCorrelation(new GroupCorrelation(3, null)).flow(flow).build();
    Event event2 = Event.builder(executionContext).message(message2).flow(flow).build();
    Event event3 = Event.builder(executionContext).message(message3).flow(flow).build();

    assertNull(router.process(event1));
    assertNull(router.process(event2));

    Event resultEvent = router.process(event3);

    assertNotNull(sensingMessageProcessor.event);
    assertThat(resultEvent, equalTo(sensingMessageProcessor.event));

    InternalMessage nextMessage = sensingMessageProcessor.event.getMessage();
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
    Flow flow = getTestFlowWithComponent("test", Apple.class);
    assertNotNull(flow);

    SimpleCollectionAggregator router = new SimpleCollectionAggregator();
    SensingNullMessageProcessor sensingMessageProcessor = getSensingNullMessageProcessor();
    router.setListener(sensingMessageProcessor);
    router.setMuleContext(muleContext);
    router.setFlowConstruct(flow);
    router.initialise();

    EventContext executionContext = DefaultEventContext.create(flow, TEST_CONNECTOR, "foo");
    InternalMessage message1 = InternalMessage.of("test event A");

    Event event1 =
        Event.builder(executionContext).message(message1).groupCorrelation(new GroupCorrelation(1, null)).flow(flow).build();

    Event resultEvent = router.process(event1);

    assertNotNull(sensingMessageProcessor.event);
    assertThat(resultEvent, equalTo(sensingMessageProcessor.event));

    InternalMessage nextMessage = sensingMessageProcessor.event.getMessage();
    assertNotNull(nextMessage);
    assertTrue(nextMessage.getPayload().getValue() instanceof List<?>);
    List<InternalMessage> payload = (List<InternalMessage>) nextMessage.getPayload().getValue();
    assertEquals(1, payload.size());
    assertEquals("test event A", payload.get(0).getPayload().getValue());
  }

  @Test
  public void testAggregateMessageCollections() throws Exception {
    Flow flow = getTestFlowWithComponent("test", Apple.class);
    assertNotNull(flow);

    SimpleCollectionAggregator router = new SimpleCollectionAggregator();
    router.setMuleContext(muleContext);
    router.setFlowConstruct(flow);
    router.initialise();

    EventContext executionContext = DefaultEventContext.create(flow, TEST_CONNECTOR, "foo");

    InternalMessage message1 = InternalMessage.builder().payload("test event A").build();
    InternalMessage message2 = InternalMessage.builder().payload("test event B").build();
    InternalMessage message3 = InternalMessage.builder().payload("test event C").build();
    InternalMessage message4 = InternalMessage.builder().payload("test event D").build();
    List<InternalMessage> list = new ArrayList<>();
    List<InternalMessage> list2 = new ArrayList<>();
    list.add(message1);
    list.add(message2);
    list2.add(message3);
    list2.add(message4);
    InternalMessage messageCollection1 = InternalMessage.builder().payload(list).build();
    InternalMessage messageCollection2 = InternalMessage.builder().payload(list2).build();

    Event event1 =
        Event.builder(executionContext).message(messageCollection1).groupCorrelation(new GroupCorrelation(2, null)).flow(flow)
            .build();
    Event event2 =
        Event.builder(executionContext).message(messageCollection2).groupCorrelation(new GroupCorrelation(2, null)).flow(flow)
            .build();

    assertNull(router.process(event1));
    Event resultEvent = router.process(event2);
    assertNotNull(resultEvent);
    InternalMessage resultMessage = resultEvent.getMessage();
    assertNotNull(resultMessage);
    List<InternalMessage> payload = (List<InternalMessage>) resultMessage.getPayload().getValue();
    assertEquals(2, payload.size());

    assertEquals("test event A", ((List<InternalMessage>) payload.get(0).getPayload().getValue()).get(0).getPayload().getValue());
    assertEquals("test event B", ((List<InternalMessage>) payload.get(0).getPayload().getValue()).get(1).getPayload().getValue());
    assertEquals("test event C", ((List<InternalMessage>) payload.get(1).getPayload().getValue()).get(0).getPayload().getValue());
    assertEquals("test event D", ((List<InternalMessage>) payload.get(1).getPayload().getValue()).get(1).getPayload().getValue());

  }

}
