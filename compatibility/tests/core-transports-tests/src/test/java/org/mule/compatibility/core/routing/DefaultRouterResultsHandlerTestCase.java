/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.routing;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.compatibility.core.DefaultMuleEventEndpointUtils.createEventUsingInboundEndpoint;
import static org.mule.runtime.core.api.MessageExchangePattern.ONE_WAY;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.endpoint.MuleEndpointURI;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.routing.RouterResultsHandler;
import org.mule.runtime.core.routing.DefaultRouterResultsHandler;
import org.mule.runtime.core.transaction.MuleTransactionConfig;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class DefaultRouterResultsHandlerTestCase extends AbstractMuleContextEndpointTestCase {

  protected RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();
  protected MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
  protected MuleSession session = mock(MuleSession.class);
  protected InboundEndpoint endpoint = mock(InboundEndpoint.class);
  protected Flow flow = mock(Flow.class);
  private EventContext context;

  @Before
  public void setupMocks() throws Exception {
    when(endpoint.getEndpointURI()).thenReturn(new MuleEndpointURI("test://test", muleContext));
    when(endpoint.getTransactionConfig()).thenReturn(new MuleTransactionConfig());
    when(endpoint.getExchangePattern()).thenReturn(ONE_WAY);
    when(flow.getMuleContext()).thenReturn(muleContext);
    when(muleContext.getConfiguration()).thenReturn(mock(MuleConfiguration.class));
    context = DefaultEventContext.create(flow, TEST_CONNECTOR);
  }

  @Test
  public void aggregateNoEvent() {
    Event result = resultsHandler.aggregateResults(Collections.<Event>singletonList(null), mock(Event.class));
    assertNull(result);
  }

  @Test
  public void aggregateSingleEvent() {

    InternalMessage message1 = InternalMessage.builder().payload("test event A").build();
    Event.Builder eventBuilder1 = Event.builder(context).flow(flow).addVariable("key1", "value1");
    Event event1 = createEventUsingInboundEndpoint(eventBuilder1, message1, endpoint);
    event1.getSession().setProperty("key", "value");

    InternalMessage message2 = InternalMessage.builder().payload("test event B").build();
    Event.Builder eventBuilder2 = Event.builder(context).flow(flow).addVariable("key2", "value2");
    Event event2 = createEventUsingInboundEndpoint(eventBuilder2, message2, endpoint);
    event2.getSession().setProperty("key", "valueNEW");
    event2.getSession().setProperty("key1", "value1");

    Event result = resultsHandler.aggregateResults(Collections.<Event>singletonList(event2), event1);
    assertSame(event2, result);

    // Because same event instance is returned rather than MessageCollection
    // don't copy invocation properties
    assertThat(result.getVariableNames(), not(hasItem("key1")));
    assertEquals("value2", result.getVariable("key2").getValue());

    assertEquals("valueNEW", result.getSession().getProperty("key"));
    assertEquals("value1", result.getSession().getProperty("key1"));

  }

  @Test
  public void aggregateMultipleEvents() throws Exception {
    DataType simpleDateType1 = DataType.builder().type(String.class).mediaType("text/plain").build();
    InternalMessage message1 = InternalMessage.builder().payload("test event A").build();
    InternalMessage message2 = InternalMessage.builder().payload("test event B").build();
    InternalMessage message3 = InternalMessage.builder().payload("test event C").build();
    Event.Builder eventBuilder1 =
        Event.builder(context).flow(flow).addVariable("key1", "value1", simpleDateType1);
    Event event1 = createEventUsingInboundEndpoint(eventBuilder1, message1, endpoint);
    MuleSession session = event1.getSession();

    Event.Builder eventBuilder2 = Event.builder(context).flow(flow).session(session)
        .addVariable("key2", "value2", simpleDateType1);
    Event event2 = createEventUsingInboundEndpoint(eventBuilder2, message2, endpoint);

    Event.Builder eventBuilder3 = Event.builder(context).message(message3).flow(flow).session(session)
        .addVariable("key3", "value3", simpleDateType1);
    Event event3 = createEventUsingInboundEndpoint(eventBuilder3, message3, endpoint);

    event1.getSession().setProperty("key", "value");
    event2.getSession().setProperty("key1", "value1");
    event2.getSession().setProperty("key2", "value2");
    event3.getSession().setProperty("KEY2", "value2NEW");
    event3.getSession().setProperty("key3", "value3");

    List<Event> events = new ArrayList<>();
    events.add(event2);
    events.add(event3);

    Event result = resultsHandler.aggregateResults(events, event1);
    assertNotNull(result);
    assertEquals(2, ((List<InternalMessage>) result.getMessage().getPayload().getValue()).size());
    assertTrue(result.getMessage().getPayload().getValue() instanceof List<?>);
    assertEquals(message2, ((List<InternalMessage>) result.getMessage().getPayload().getValue()).get(0));
    assertEquals(message3, ((List<InternalMessage>) result.getMessage().getPayload().getValue()).get(1));

    // Because a new MuleMessageCollection is created, propagate properties from
    // original event
    assertThat(result.getVariable("key1").getValue(), equalTo("value1"));
    assertThat(result.getVariable("key1").getDataType(), equalTo(simpleDateType1));
    assertThat(result.getVariable("key2").getValue(), equalTo("value2"));
    assertThat(result.getVariable("key3").getValue(), equalTo("value3"));

    // Root id
    assertEquals(event1.getCorrelationId(), result.getCorrelationId());

    assertEquals("value", result.getSession().getProperty("key"));
    assertEquals("value1", result.getSession().getProperty("key1"));
    assertEquals("value2NEW", result.getSession().getProperty("key2"));
    assertEquals("value3", result.getSession().getProperty("key3"));
    assertNull(result.getSession().getProperty("key4"));
  }

  @Test
  public void aggregateMultipleEventsAllButOneNull() {
    InternalMessage message1 = InternalMessage.builder().payload("test event A").build();
    InternalMessage message2 = InternalMessage.builder().payload("test event B").build();
    Event.Builder eventBuilder1 = Event.builder(context).flow(flow).addVariable("key", "value");
    Event event1 = createEventUsingInboundEndpoint(eventBuilder1, message1, endpoint);

    Event.Builder eventBuilder2 = Event.builder(context).flow(flow).addVariable("key2", "value2");
    Event event2 = createEventUsingInboundEndpoint(eventBuilder2, message2, endpoint);
    List<Event> events = new ArrayList<>();
    events.add(null);
    events.add(event2);

    Event result = resultsHandler.aggregateResults(events, event1);
    assertSame(event2, result);

    // Because same event instance is returned rather than MessageCollection
    // don't copy invocation properties
    assertThat(result.getVariableNames(), not(hasItem("key1")));
    assertEquals("value2", result.getVariable("key2").getValue());
  }

  @Test
  public void aggregateSingleMuleMessageCollection() {
    InternalMessage message1 = InternalMessage.builder().payload("test event A").build();
    Event.Builder eventBuilder1 = Event.builder(context).flow(flow).addVariable("key1", "value1");
    Event event1 = createEventUsingInboundEndpoint(eventBuilder1, message1, endpoint);

    InternalMessage message2 = InternalMessage.builder().payload("test event B").build();
    InternalMessage message3 = InternalMessage.builder().payload("test event C").build();

    List<InternalMessage> list = new ArrayList<>();
    list.add(message2);
    list.add(message3);
    InternalMessage messageCollection = InternalMessage.builder().payload(list).build();
    Event.Builder eventBuilder2 = Event.builder(context).flow(flow).addVariable("key2", "value2");
    Event event2 = createEventUsingInboundEndpoint(eventBuilder2, messageCollection, endpoint);

    Event result = resultsHandler.aggregateResults(Collections.<Event>singletonList(event2), event1);
    assertSame(event2, result);

    // Because same event instance is returned rather than MessageCollection
    // don't copy invocation properties
    assertThat(result.getVariableNames(), not(hasItem("key1")));
    assertEquals("value2", result.getVariable("key2").getValue());
  }

  @Test
  public void aggregateMultipleMuleMessageCollections() {
    InternalMessage message1 = InternalMessage.builder().payload("test event A").build();
    Event event1 = Event.builder(context).message(message1).flow(flow).addVariable("key1", "value1").build();

    InternalMessage message2 = InternalMessage.builder().payload("test event B").build();
    InternalMessage message3 = InternalMessage.builder().payload("test event C").build();
    InternalMessage message4 = InternalMessage.builder().payload("test event D").build();
    InternalMessage message5 = InternalMessage.builder().payload("test event E").build();

    List<InternalMessage> list = new ArrayList<>();
    list.add(message2);
    list.add(message3);
    InternalMessage messageCollection = InternalMessage.builder().payload(list).build();
    Event.Builder eventBuilder2 = Event.builder(context).flow(flow).addVariable("key2", "value2");
    Event event2 = createEventUsingInboundEndpoint(eventBuilder2, messageCollection, endpoint);

    List<InternalMessage> list2 = new ArrayList<>();
    list.add(message4);
    list.add(message5);
    InternalMessage messageCollection2 = InternalMessage.builder().payload(list2).build();
    Event.Builder eventBuilder3 =
        Event.builder(context).flow(flow).addVariable("key3", "value3");
    Event event3 = createEventUsingInboundEndpoint(eventBuilder3, messageCollection2, endpoint);

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
    assertEquals(event1.getCorrelationId(), result.getCorrelationId());
  }

}
