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

import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MessageContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.message.Correlation;
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

    Flow flow = getTestFlow("test", Apple.class);
    assertNotNull(flow);

    SimpleCollectionAggregator router = new SimpleCollectionAggregator();
    SensingNullMessageProcessor sensingMessageProcessor = getSensingNullMessageProcessor();
    router.setListener(sensingMessageProcessor);
    router.setMuleContext(muleContext);
    router.setFlowConstruct(flow);
    router.initialise();

    MessageContext executionContext = DefaultMessageContext.create(flow, TEST_CONNECTOR, "foo");

    MuleMessage message1 = MuleMessage.builder().payload("test event A").build();
    MuleMessage message2 = MuleMessage.builder().payload("test event B").build();
    MuleMessage message3 = MuleMessage.builder().payload("test event C").build();

    DefaultMuleEvent event1 = (DefaultMuleEvent) MuleEvent.builder(executionContext).message(message1).flow(flow).build();
    DefaultMuleEvent event2 = (DefaultMuleEvent) MuleEvent.builder(executionContext).message(message2).flow(flow).build();
    DefaultMuleEvent event3 = (DefaultMuleEvent) MuleEvent.builder(executionContext).message(message3).flow(flow).build();
    event1.setCorrelation(new Correlation(3, null));

    assertNull(router.process(event1));
    assertNull(router.process(event2));

    MuleEvent resultEvent = router.process(event3);

    assertNotNull(sensingMessageProcessor.event);
    assertThat(resultEvent, equalTo(sensingMessageProcessor.event));

    MuleMessage nextMessage = sensingMessageProcessor.event.getMessage();
    assertNotNull(nextMessage);
    assertTrue(nextMessage.getPayload() instanceof List<?>);
    List<MuleMessage> list = nextMessage.getPayload();
    assertEquals(3, list.size());
    String[] results = new String[3];
    list.stream().map(MuleMessage::getPayload).collect(toList()).toArray(results);
    // Need to sort result because of MULE-5998
    Arrays.sort(results);
    assertEquals("test event A", results[0]);
    assertEquals("test event B", results[1]);
    assertEquals("test event C", results[2]);
  }

  @Test
  public void testAggregateSingleEvent() throws Exception {
    Flow flow = getTestFlow("test", Apple.class);
    assertNotNull(flow);

    SimpleCollectionAggregator router = new SimpleCollectionAggregator();
    SensingNullMessageProcessor sensingMessageProcessor = getSensingNullMessageProcessor();
    router.setListener(sensingMessageProcessor);
    router.setMuleContext(muleContext);
    router.setFlowConstruct(flow);
    router.initialise();

    MessageContext executionContext = DefaultMessageContext.create(flow, TEST_CONNECTOR, "foo");
    MuleMessage message1 = MuleMessage.of("test event A");

    DefaultMuleEvent event1 = (DefaultMuleEvent) MuleEvent.builder(executionContext).message(message1).flow(flow).build();
    event1.setCorrelation(new Correlation(1, null));

    MuleEvent resultEvent = router.process(event1);

    assertNotNull(sensingMessageProcessor.event);
    assertThat(resultEvent, equalTo(sensingMessageProcessor.event));

    MuleMessage nextMessage = sensingMessageProcessor.event.getMessage();
    assertNotNull(nextMessage);
    assertTrue(nextMessage.getPayload() instanceof List<?>);
    List<MuleMessage> payload = nextMessage.getPayload();
    assertEquals(1, payload.size());
    assertEquals("test event A", payload.get(0).getPayload());
  }

  @Test
  public void testAggregateMessageCollections() throws Exception {
    Flow flow = getTestFlow("test", Apple.class);
    assertNotNull(flow);

    SimpleCollectionAggregator router = new SimpleCollectionAggregator();
    router.setMuleContext(muleContext);
    router.setFlowConstruct(flow);
    router.initialise();

    MessageContext executionContext = DefaultMessageContext.create(flow, TEST_CONNECTOR, "foo");

    MuleMessage message1 = MuleMessage.builder().payload("test event A").build();
    MuleMessage message2 = MuleMessage.builder().payload("test event B").build();
    MuleMessage message3 = MuleMessage.builder().payload("test event C").build();
    MuleMessage message4 = MuleMessage.builder().payload("test event D").build();
    List<MuleMessage> list = new ArrayList<>();
    List<MuleMessage> list2 = new ArrayList<>();
    list.add(message1);
    list.add(message2);
    list2.add(message3);
    list2.add(message4);
    MuleMessage messageCollection1 = MuleMessage.builder().payload(list).build();
    MuleMessage messageCollection2 = MuleMessage.builder().payload(list2).build();

    DefaultMuleEvent event1 =
        (DefaultMuleEvent) MuleEvent.builder(executionContext).message(messageCollection1).flow(flow).build();
    event1.setCorrelation(new Correlation(2, null));
    DefaultMuleEvent event2 =
        (DefaultMuleEvent) MuleEvent.builder(executionContext).message(messageCollection2).flow(flow).build();
    event2.setCorrelation(new Correlation(2, null));

    assertNull(router.process(event1));
    MuleEvent resultEvent = router.process(event2);
    assertNotNull(resultEvent);
    MuleMessage resultMessage = resultEvent.getMessage();
    assertNotNull(resultMessage);
    List<MuleMessage> payload = (List<MuleMessage>) resultMessage.getPayload();
    assertEquals(2, payload.size());

    assertEquals("test event A", ((List<MuleMessage>) payload.get(0).getPayload()).get(0).getPayload());
    assertEquals("test event B", ((List<MuleMessage>) payload.get(0).getPayload()).get(1).getPayload());
    assertEquals("test event C", ((List<MuleMessage>) payload.get(1).getPayload()).get(0).getPayload());
    assertEquals("test event D", ((List<MuleMessage>) payload.get(1).getPayload()).get(1).getPayload());

  }

}
