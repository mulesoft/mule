/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.DefaultMessageExecutionContext;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

public class SimpleCollectionAggregatorTestCase extends AbstractMuleContextTestCase {

  public SimpleCollectionAggregatorTestCase() {
    setStartContext(true);
  }

  @Ignore
  @Test
  public void testAggregateMultipleEvents() throws Exception {
    MuleSession session1 = getTestSession(getTestFlow(), muleContext);
    session1.setProperty("key1", "value1");
    MuleSession session2 = getTestSession(getTestFlow(), muleContext);
    session1.setProperty("key1", "value1NEW");
    session1.setProperty("key2", "value2");
    MuleSession session3 = getTestSession(getTestFlow(), muleContext);
    session1.setProperty("key3", "value3");

    Flow flow = getTestFlow("test", Apple.class);
    assertNotNull(flow);

    SimpleCollectionAggregator router = new SimpleCollectionAggregator();
    SensingNullMessageProcessor sensingMessageProcessor = getSensingNullMessageProcessor();
    router.setListener(sensingMessageProcessor);
    router.setMuleContext(muleContext);
    router.setFlowConstruct(flow);
    router.initialise();

    MuleMessage message1 = MuleMessage.builder().payload("test event A").build();
    MuleMessage message2 = MuleMessage.builder().payload("test event B").build();
    MuleMessage message3 = MuleMessage.builder().payload("test event C").build();
    message1 = MuleMessage.builder(message1).correlationId(message1.getUniqueId()).correlationGroupSize(3).build();
    message2 = MuleMessage.builder(message2).correlationId(message1.getUniqueId()).build();
    message3 = MuleMessage.builder(message1).correlationId(message1.getUniqueId()).build();

    MuleEvent event1 =
        new DefaultMuleEvent(new DefaultMessageExecutionContext(muleContext.getUniqueIdString(), null), message1, flow, session1);
    MuleEvent event2 =
        new DefaultMuleEvent(new DefaultMessageExecutionContext(muleContext.getUniqueIdString(), null), message2, flow, session2);
    MuleEvent event3 =
        new DefaultMuleEvent(new DefaultMessageExecutionContext(muleContext.getUniqueIdString(), null), message3, flow, session3);

    assertNull(router.process(event1));
    assertNull(router.process(event2));
    assertSame(VoidMuleEvent.getInstance(), router.process(event3));

    assertNotNull(sensingMessageProcessor.event);
    MuleMessage nextMessage = sensingMessageProcessor.event.getMessage();
    assertNotNull(nextMessage);
    assertTrue(nextMessage.getPayload() instanceof List<?>);
    List<MuleMessage> list = (List<MuleMessage>) nextMessage.getPayload();
    assertEquals(3, list.size());
    String[] results = new String[3];
    list.stream().map(MuleMessage::getPayload).collect(toList()).toArray(results);
    // Need to sort result because of MULE-5998
    Arrays.sort(results);
    assertEquals("test event A", results[0]);
    assertEquals("test event B", results[1]);
    assertEquals("test event C", results[2]);

    // Assert that session was merged correctly
    assertEquals(3, sensingMessageProcessor.event.getSession().getPropertyNamesAsSet().size());
    assertEquals("value1NEW", sensingMessageProcessor.event.getSession().getProperty("key1"));
    assertEquals("value2", sensingMessageProcessor.event.getSession().getProperty("key2"));
    assertEquals("value3", sensingMessageProcessor.event.getSession().getProperty("key3"));
  }

  @Ignore
  @Test
  public void testAggregateSingleEvent() throws Exception {
    MuleSession session = getTestSession(getTestFlow(), muleContext);
    Flow flow = getTestFlow("test", Apple.class);
    assertNotNull(flow);

    SimpleCollectionAggregator router = new SimpleCollectionAggregator();
    SensingNullMessageProcessor sensingMessageProcessor = getSensingNullMessageProcessor();
    router.setListener(sensingMessageProcessor);
    router.setMuleContext(muleContext);
    router.setFlowConstruct(flow);
    router.initialise();

    MuleMessage message1 = MuleMessage.builder().payload("test event A").build();
    message1 = MuleMessage.builder(message1).correlationId(message1.getUniqueId()).correlationGroupSize(1).build();

    MuleEvent event1 =
        new DefaultMuleEvent(new DefaultMessageExecutionContext(muleContext.getUniqueIdString(), null), message1, flow);

    MuleEvent resultEvent = router.process(event1);
    assertSame(VoidMuleEvent.getInstance(), resultEvent);

    assertNotNull(sensingMessageProcessor.event);
    MuleMessage nextMessage = sensingMessageProcessor.event.getMessage();
    assertNotNull(nextMessage);
    assertTrue(nextMessage.getPayload() instanceof List<?>);
    List<MuleMessage> payload = (List<MuleMessage>) nextMessage.getPayload();
    assertEquals(1, payload.size());
    assertEquals("test event A", payload.get(0).getPayload());
  }

  @Test
  public void testAggregateMessageCollections() throws Exception {
    MuleSession session = getTestSession(getTestFlow(), muleContext);
    Flow flow = getTestFlow("test", Apple.class);
    assertNotNull(flow);

    SimpleCollectionAggregator router = new SimpleCollectionAggregator();
    router.setMuleContext(muleContext);
    router.setFlowConstruct(flow);
    router.initialise();

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

    messageCollection1 =
        MuleMessage.builder(messageCollection1).correlationGroupSize(2).correlationId(messageCollection1.getUniqueId()).build();
    messageCollection2 =
        MuleMessage.builder(messageCollection2).correlationGroupSize(2).correlationId(messageCollection1.getUniqueId()).build();

    MuleEvent event1 =
        new DefaultMuleEvent(new DefaultMessageExecutionContext(muleContext.getUniqueIdString(), null), messageCollection1, flow);
    MuleEvent event2 =
        new DefaultMuleEvent(new DefaultMessageExecutionContext(muleContext.getUniqueIdString(), null), messageCollection2, flow);

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
