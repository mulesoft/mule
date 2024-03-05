/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import static java.util.Collections.singletonList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.construct.DefaultFlowBuilder;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.routing.DefaultRouterResultsHandler;
import org.mule.runtime.core.privileged.routing.RouterResultsHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DefaultRouterResultsHandlerTestCase extends AbstractMuleTestCase {

  protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
  protected MuleContext muleContext = mockContextWithServices();
  protected DefaultFlowBuilder.DefaultFlow flow = mock(DefaultFlowBuilder.DefaultFlow.class);
  private EventContext context;

  @Before
  public void setupMocks() throws Exception {
    when(flow.getMuleContext()).thenReturn(muleContext);

    when(muleContext.getConfiguration()).thenReturn(mock(MuleConfiguration.class));

    context = create(flow, TEST_CONNECTOR_LOCATION);
  }

  @Test
  public void aggregateNoEvent() {
    CoreEvent result =
        resultsHandler.aggregateResults(Collections.<CoreEvent>singletonList(null), mock(CoreEvent.class));
    assertNull(result);
  }

  @Test
  public void aggregateSingleEvent() {

    Message message1 = Message.of("test event A");
    CoreEvent event1 = CoreEvent.builder(context).message(message1).addVariable("key1", "value1").build();

    Message message2 = Message.of("test event B");
    CoreEvent event2 = CoreEvent.builder(context).message(message2).addVariable("key2", "value2").build();

    CoreEvent result = resultsHandler.aggregateResults(singletonList(event2), event1);
    assertSame(event2, result);

    // Because same event instance is returned rather than MessageCollection
    // don't copy invocation properties
    assertThat(result.getVariables().keySet(), not(hasItem("key1")));
    assertThat(result.getVariables().get("key2").getValue(), equalTo("value2"));

    PrivilegedEvent privilegedResult = (PrivilegedEvent) result;
  }

  @Test
  public void aggregateMultipleEvents() throws Exception {
    DataType simpleDateType1 = DataType.builder().type(String.class).mediaType("text/plain").build();
    Message message1 = Message.of("test event A");
    Message message2 = Message.of("test event B");
    Message message3 = Message.of("test event C");
    PrivilegedEvent event1 =
        PrivilegedEvent.builder(context).message(message1).addVariable("key1", "value1", simpleDateType1).build();
    PrivilegedEvent event2 = PrivilegedEvent.builder(context).message(message2)
        .addVariable("key2", "value2", simpleDateType1).build();
    CoreEvent event3 = CoreEvent.builder(context).message(message3)
        .addVariable("key3", "value3", simpleDateType1).build();

    List<CoreEvent> events = new ArrayList<>();
    events.add(event2);
    events.add(event3);

    PrivilegedEvent result = (PrivilegedEvent) resultsHandler.aggregateResults(events, event1);
    assertThat(result, notNullValue());
    assertThat((List<InternalMessage>) result.getMessage().getPayload().getValue(), hasSize(2));
    assertThat(result.getMessage().getPayload().getValue(), instanceOf(List.class));
    assertThat(((List<InternalMessage>) result.getMessage().getPayload().getValue()).get(0), is(message2));
    assertThat(((List<InternalMessage>) result.getMessage().getPayload().getValue()).get(1), is(message3));

    // Because a new MuleMessageCollection is created, propagate properties from
    // original event
    assertThat(result.getVariables().get("key1").getValue(), equalTo("value1"));
    assertThat(result.getVariables().get("key1").getDataType(), equalTo(simpleDateType1));
    assertThat(result.getVariables().get("key2").getValue(), equalTo("value2"));
    assertThat(result.getVariables().get("key3").getValue(), equalTo("value3"));

    // Root id
    assertThat(result.getCorrelationId(), equalTo(event1.getCorrelationId()));
  }

  @Test
  public void aggregateMultipleEventsAllButOneNull() {
    Message message1 = Message.of("test event A");
    Message message2 = Message.of("test event B");
    CoreEvent event1 = CoreEvent.builder(context).message(message1).addVariable("key", "value").build();
    CoreEvent event2 = CoreEvent.builder(context).message(message2).addVariable("key2", "value2").build();
    List<CoreEvent> events = new ArrayList<>();
    events.add(null);
    events.add(event2);

    CoreEvent result = resultsHandler.aggregateResults(events, event1);
    assertSame(event2, result);

    // Because same event instance is returned rather than MessageCollection
    // don't copy invocation properties
    assertThat(result.getVariables().keySet(), not(hasItem("key1")));
    assertThat(result.getVariables().get("key2").getValue(), equalTo("value2"));
  }

  @Test
  public void aggregateSingleMuleMessageCollection() {
    Message message1 = Message.of("test event A");
    CoreEvent event1 = CoreEvent.builder(context).message(message1).addVariable("key1", "value1").build();

    Message message2 = Message.of("test event B");
    Message message3 = Message.of("test event C");

    List<Message> list = new ArrayList<>();
    list.add(message2);
    list.add(message3);
    Message messageCollection = Message.of(list);
    CoreEvent event2 =
        CoreEvent.builder(context).message(messageCollection).addVariable("key2", "value2").build();

    CoreEvent result = resultsHandler.aggregateResults(Collections.<CoreEvent>singletonList(event2), event1);
    assertSame(event2, result);

    // Because same event instance is returned rather than MessageCollection
    // don't copy invocation properties
    assertThat(result.getVariables().keySet(), not(hasItem("key1")));
    assertThat(result.getVariables().get("key2").getValue(), equalTo("value2"));
  }

  @Test
  public void aggregateMultipleMuleMessageCollections() {
    Message message1 = Message.of("test event A");
    CoreEvent event1 = CoreEvent.builder(context).message(message1).addVariable("key1", "value1").build();

    Message message2 = Message.of("test event B");
    Message message3 = Message.of("test event C");
    Message message4 = Message.of("test event D");
    Message message5 = Message.of("test event E");

    List<Message> list = new ArrayList<>();
    list.add(message2);
    list.add(message3);
    Message messageCollection = Message.of(list);
    CoreEvent event2 =
        CoreEvent.builder(context).message(messageCollection).addVariable("key2", "value2").build();

    List<InternalMessage> list2 = new ArrayList<>();
    list.add(message4);
    list.add(message5);
    Message messageCollection2 = Message.of(list2);
    CoreEvent event3 =
        CoreEvent.builder(context).message(messageCollection2).addVariable("key3", "value3").build();

    List<CoreEvent> events = new ArrayList<>();
    events.add(event2);
    events.add(event3);

    CoreEvent result = resultsHandler.aggregateResults(events, event1);
    assertNotNull(result);
    assertEquals(2, ((List<InternalMessage>) result.getMessage().getPayload().getValue()).size());
    assertTrue(result.getMessage().getPayload().getValue() instanceof List<?>);
    assertEquals(messageCollection, ((List<InternalMessage>) result.getMessage().getPayload().getValue()).get(0));
    assertEquals(messageCollection2, ((List<InternalMessage>) result.getMessage().getPayload().getValue()).get(1));

    // Because a new MuleMessageCollection is created, propagate properties from
    // original event
    assertThat(result.getVariables().get("key1").getValue(), equalTo("value1"));
    assertThat(result.getVariables().get("key2").getValue(), equalTo("value2"));
    assertThat(result.getVariables().get("key3").getValue(), equalTo("value3"));

    // Root id
    assertThat(result.getCorrelationId(), equalTo(event1.getCorrelationId()));
  }

}
