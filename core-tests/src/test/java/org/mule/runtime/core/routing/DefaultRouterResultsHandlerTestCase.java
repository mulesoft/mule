/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.routing.RouterResultsHandler;
import org.mule.runtime.core.internal.construct.DefaultFlowBuilder;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.processor.strategy.BlockingProcessingStrategyFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DefaultRouterResultsHandlerTestCase extends AbstractMuleTestCase {

  protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
  protected MuleContext muleContext = mockContextWithServices();
  protected MuleSession session = mock(MuleSession.class);
  protected DefaultFlowBuilder.DefaultFlow flow = mock(DefaultFlowBuilder.DefaultFlow.class);
  private EventContext context;

  @Before
  public void setupMocks() throws Exception {
    when(flow.getProcessingStrategyFactory()).thenReturn(new BlockingProcessingStrategyFactory());
    when(flow.getMuleContext()).thenReturn(muleContext);

    when(muleContext.getConfiguration()).thenReturn(mock(MuleConfiguration.class));

    context = DefaultEventContext.create(flow, TEST_CONNECTOR_LOCATION);
  }

  @Test
  public void aggregateNoEvent() {
    Event result = resultsHandler.aggregateResults(Collections.<Event>singletonList(null), mock(Event.class));
    assertNull(result);
  }

  @Test
  public void aggregateSingleEvent() {

    Message message1 = Message.of("test event A");
    Event event1 = Event.builder(context).message(message1).flow(flow).addVariable("key1", "value1").build();
    event1.getSession().setProperty("key", "value");

    Message message2 = Message.of("test event B");
    Event event2 = Event.builder(context).message(message2).flow(flow).addVariable("key2", "value2").build();
    event2.getSession().setProperty("key", "valueNEW");
    event2.getSession().setProperty("key1", "value1");

    Event result = resultsHandler.aggregateResults(Collections.<Event>singletonList(event2), event1);
    assertSame(event2, result);

    // Because same event instance is returned rather than MessageCollection
    // don't copy invocation properties
    assertThat(result.getVariableNames(), not(hasItem("key1")));
    assertThat(result.getVariable("key2").getValue(), equalTo("value2"));

    assertThat(result.getSession().getProperty("key"), equalTo("valueNEW"));
    assertThat(result.getSession().getProperty("key1"), equalTo("value1"));

  }

  @Test
  public void aggregateMultipleEvents() throws Exception {
    DataType simpleDateType1 = DataType.builder().type(String.class).mediaType("text/plain").build();
    Message message1 = Message.of("test event A");
    Message message2 = Message.of("test event B");
    Message message3 = Message.of("test event C");
    Event event1 =
        Event.builder(context).message(message1).flow(flow).addVariable("key1", "value1", simpleDateType1).build();
    MuleSession session = event1.getSession();
    Event event2 = Event.builder(context).message(message2).flow(flow).session(session)
        .addVariable("key2", "value2", simpleDateType1).build();
    Event event3 = Event.builder(context).message(message3).flow(flow).session(session)
        .addVariable("key3", "value3", simpleDateType1).build();
    event1.getSession().setProperty("key", "value");
    event2.getSession().setProperty("key1", "value1");
    event2.getSession().setProperty("key2", "value2");
    event3.getSession().setProperty("KEY2", "value2NEW");
    event3.getSession().setProperty("key3", "value3");

    List<Event> events = new ArrayList<>();
    events.add(event2);
    events.add(event3);

    Event result = resultsHandler.aggregateResults(events, event1);
    assertThat(result, notNullValue());
    assertThat((List<InternalMessage>) result.getMessage().getPayload().getValue(), hasSize(2));
    assertThat(result.getMessage().getPayload().getValue(), instanceOf(List.class));
    assertThat(((List<InternalMessage>) result.getMessage().getPayload().getValue()).get(0), is(message2));
    assertThat(((List<InternalMessage>) result.getMessage().getPayload().getValue()).get(1), is(message3));

    // Because a new MuleMessageCollection is created, propagate properties from
    // original event
    assertThat(result.getVariable("key1").getValue(), equalTo("value1"));
    assertThat(result.getVariable("key1").getDataType(), equalTo(simpleDateType1));
    assertThat(result.getVariable("key2").getValue(), equalTo("value2"));
    assertThat(result.getVariable("key3").getValue(), equalTo("value3"));

    // Root id
    assertThat(result.getCorrelationId(), equalTo(event1.getCorrelationId()));

    assertThat(result.getSession().getProperty("key"), is("value"));
    assertThat(result.getSession().getProperty("key1"), is("value1"));
    assertThat(result.getSession().getProperty("key2"), is("value2NEW"));
    assertThat(result.getSession().getProperty("key3"), is("value3"));
    assertThat(result.getSession().getProperty("key4"), nullValue());
  }

  @Test
  public void aggregateMultipleEventsAllButOneNull() {
    Message message1 = Message.of("test event A");
    Message message2 = Message.of("test event B");
    Event event1 = Event.builder(context).message(message1).flow(flow).addVariable("key", "value").build();
    Event event2 = Event.builder(context).message(message2).flow(flow).addVariable("key2", "value2").build();
    List<Event> events = new ArrayList<>();
    events.add(null);
    events.add(event2);

    Event result = resultsHandler.aggregateResults(events, event1);
    assertSame(event2, result);

    // Because same event instance is returned rather than MessageCollection
    // don't copy invocation properties
    assertThat(result.getVariableNames(), not(hasItem("key1")));
    assertThat(result.getVariable("key2").getValue(), equalTo("value2"));
  }

  @Test
  public void aggregateSingleMuleMessageCollection() {
    Message message1 = Message.of("test event A");
    Event event1 = Event.builder(context).message(message1).flow(flow).addVariable("key1", "value1").build();

    Message message2 = Message.of("test event B");
    Message message3 = Message.of("test event C");

    List<Message> list = new ArrayList<>();
    list.add(message2);
    list.add(message3);
    Message messageCollection = Message.of(list);
    Event event2 = Event.builder(context).message(messageCollection).flow(flow).addVariable("key2", "value2").build();

    Event result = resultsHandler.aggregateResults(Collections.<Event>singletonList(event2), event1);
    assertSame(event2, result);

    // Because same event instance is returned rather than MessageCollection
    // don't copy invocation properties
    assertThat(result.getVariableNames(), not(hasItem("key1")));
    assertThat(result.getVariable("key2").getValue(), equalTo("value2"));
  }

  @Test
  public void aggregateMultipleMuleMessageCollections() {
    Message message1 = Message.of("test event A");
    Event event1 = Event.builder(context).message(message1).flow(flow).addVariable("key1", "value1").build();

    Message message2 = Message.of("test event B");
    Message message3 = Message.of("test event C");
    Message message4 = Message.of("test event D");
    Message message5 = Message.of("test event E");

    List<Message> list = new ArrayList<>();
    list.add(message2);
    list.add(message3);
    Message messageCollection = Message.of(list);
    Event event2 = Event.builder(context).message(messageCollection).flow(flow).addVariable("key2", "value2").build();

    List<InternalMessage> list2 = new ArrayList<>();
    list.add(message4);
    list.add(message5);
    Message messageCollection2 = Message.of(list2);
    Event event3 =
        Event.builder(context).message(messageCollection2).flow(flow).addVariable("key3", "value3").build();

    List<Event> events = new ArrayList<>();
    events.add(event2);
    events.add(event3);

    Event result = resultsHandler.aggregateResults(events, event1);
    assertNotNull(result);
    assertEquals(2, ((List<InternalMessage>) result.getMessage().getPayload().getValue()).size());
    assertTrue(result.getMessage().getPayload().getValue() instanceof List<?>);
    assertEquals(messageCollection, ((List<InternalMessage>) result.getMessage().getPayload().getValue()).get(0));
    assertEquals(messageCollection2, ((List<InternalMessage>) result.getMessage().getPayload().getValue()).get(1));

    // Because a new MuleMessageCollection is created, propagate properties from
    // original event
    assertThat(result.getVariable("key1").getValue(), equalTo("value1"));
    assertThat(result.getVariable("key2").getValue(), equalTo("value2"));
    assertThat(result.getVariable("key3").getValue(), equalTo("value3"));

    // Root id
    assertThat(result.getCorrelationId(), equalTo(event1.getCorrelationId()));
  }

}
