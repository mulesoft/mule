/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.compatibility.core.DefaultMuleEventEndpointUtils;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.endpoint.MuleEndpointURI;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.routing.RouterResultsHandler;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.processor.strategy.SynchronousProcessingStrategy;
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

  @Before
  public void setupMocks() throws Exception {
    when(endpoint.getEndpointURI()).thenReturn(new MuleEndpointURI("test://test", muleContext));
    when(endpoint.getTransactionConfig()).thenReturn(new MuleTransactionConfig());
    when(endpoint.getExchangePattern()).thenReturn(MessageExchangePattern.ONE_WAY);
    when(flow.getProcessingStrategy()).thenReturn(new SynchronousProcessingStrategy());
    when(flow.getMuleContext()).thenReturn(muleContext);
    when(muleContext.getConfiguration()).thenReturn(mock(MuleConfiguration.class));
  }

  @Test
  public void aggregateNoEvent() {
    MuleEvent result = resultsHandler.aggregateResults(Collections.<MuleEvent>singletonList(null), mock(MuleEvent.class));
    assertNull(result);
  }

  @Test
  public void aggregateSingleEvent() {

    MuleMessage message1 = MuleMessage.builder().payload("test event A").build();
    DefaultMuleEvent event1 = new DefaultMuleEvent(message1, flow);
    DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint(event1, endpoint);
    event1.setFlowVariable("key1", "value1");
    event1.getSession().setProperty("key", "value");

    MuleMessage message2 = MuleMessage.builder().payload("test event B").build();
    DefaultMuleEvent event2 = new DefaultMuleEvent(message2, flow);
    DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint(event2, endpoint);
    event2.setFlowVariable("key2", "value2");
    event2.getSession().setProperty("key", "valueNEW");
    event2.getSession().setProperty("key1", "value1");

    MuleEvent result = resultsHandler.aggregateResults(Collections.<MuleEvent>singletonList(event2), event1);
    assertSame(event2, result);

    // Because same event instance is returned rather than MessageCollection
    // don't copy invocation properties
    assertNull(result.getFlowVariable("key1"));
    assertEquals("value2", result.getFlowVariable("key2"));

    assertEquals("valueNEW", result.getSession().getProperty("key"));
    assertEquals("value1", result.getSession().getProperty("key1"));

  }

  @Test
  public void aggregateMultipleEvents() throws Exception {
    DataType simpleDateType1 = DataType.builder().type(String.class).mediaType("text/plain").build();
    MuleMessage message1 = MuleMessage.builder().payload("test event A").build();
    MuleMessage message2 = MuleMessage.builder().payload("test event B").build();
    MuleMessage message3 = MuleMessage.builder().payload("test event C").build();
    DefaultMuleEvent event1 = new DefaultMuleEvent(message1, flow);
    DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint(event1, endpoint);

    event1.setFlowVariable("key1", "value1", simpleDateType1);
    MuleSession session = event1.getSession();
    DefaultMuleEvent event2 = new DefaultMuleEvent(message2, flow, session);
    DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint(event2, endpoint);
    event2.setFlowVariable("key2", "value2", simpleDateType1);
    DefaultMuleEvent event3 = new DefaultMuleEvent(message3, flow, session);
    DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint(event3, endpoint);
    event3.setFlowVariable("key3", "value3", simpleDateType1);
    event1.getSession().setProperty("key", "value");
    event2.getSession().setProperty("key1", "value1");
    event2.getSession().setProperty("key2", "value2");
    event3.getSession().setProperty("KEY2", "value2NEW");
    event3.getSession().setProperty("key3", "value3");

    List<MuleEvent> events = new ArrayList<>();
    events.add(event2);
    events.add(event3);

    MuleEvent result = resultsHandler.aggregateResults(events, event1);
    assertNotNull(result);
    assertEquals(2, ((List<MuleMessage>) result.getMessage().getPayload()).size());
    assertTrue(result.getMessage().getPayload() instanceof List<?>);
    assertEquals(message2, ((List<MuleMessage>) result.getMessage().getPayload()).get(0));
    assertEquals(message3, ((List<MuleMessage>) result.getMessage().getPayload()).get(1));

    // Because a new MuleMessageCollection is created, propagate properties from
    // original event
    assertEquals("value1", result.getFlowVariable("key1"));
    assertTrue(simpleDateType1.equals(result.getFlowVariableDataType("key1")));
    assertNull(result.getFlowVariable("key2"));
    assertNull(result.getFlowVariable("key3"));

    // Root id
    assertEquals(event1.getMessage().getMessageRootId(), result.getMessage().getMessageRootId());

    assertEquals("value", result.getSession().getProperty("key"));
    assertEquals("value1", result.getSession().getProperty("key1"));
    assertEquals("value2NEW", result.getSession().getProperty("key2"));
    assertEquals("value3", result.getSession().getProperty("key3"));
    assertNull(result.getSession().getProperty("key4"));
  }

  @Test
  public void aggregateMultipleEventsAllButOneNull() {
    MuleMessage message1 = MuleMessage.builder().payload("test event A").build();
    MuleMessage message2 = MuleMessage.builder().payload("test event B").build();
    DefaultMuleEvent event1 = new DefaultMuleEvent(message1, flow);
    DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint(event1, endpoint);

    event1.setFlowVariable("key", "value");
    DefaultMuleEvent event2 = new DefaultMuleEvent(message2, flow);
    DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint(event2, endpoint);
    event2.setFlowVariable("key2", "value2");
    List<MuleEvent> events = new ArrayList<>();
    events.add(null);
    events.add(event2);

    MuleEvent result = resultsHandler.aggregateResults(events, event1);
    assertSame(event2, result);

    // Because same event instance is returned rather than MessageCollection
    // don't copy invocation properties
    assertNull(result.getFlowVariable("key1"));
    assertEquals("value2", result.getFlowVariable("key2"));
  }

  @Test
  public void aggregateSingleMuleMessageCollection() {
    MuleMessage message1 = MuleMessage.builder().payload("test event A").build();
    DefaultMuleEvent event1 = new DefaultMuleEvent(message1, flow);
    DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint(event1, endpoint);

    event1.setFlowVariable("key1", "value1");

    MuleMessage message2 = MuleMessage.builder().payload("test event B").build();
    MuleMessage message3 = MuleMessage.builder().payload("test event C").build();

    List<MuleMessage> list = new ArrayList<>();
    list.add(message2);
    list.add(message3);
    MuleMessage messageCollection = MuleMessage.builder().payload(list).build();
    DefaultMuleEvent event2 = new DefaultMuleEvent(messageCollection, flow);
    DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint(event2, endpoint);
    event2.setFlowVariable("key2", "value2");

    MuleEvent result = resultsHandler.aggregateResults(Collections.<MuleEvent>singletonList(event2), event1);
    assertSame(event2, result);

    // Because same event instance is returned rather than MessageCollection
    // don't copy invocation properties
    assertNull(result.getFlowVariable("key1"));
    assertEquals("value2", result.getFlowVariable("key2"));
  }

  @Test
  public void aggregateMultipleMuleMessageCollections() {
    MuleMessage message1 = MuleMessage.builder().payload("test event A").build();
    MuleEvent event1 = new DefaultMuleEvent(message1, flow);
    event1.setFlowVariable("key1", "value1");

    MuleMessage message2 = MuleMessage.builder().payload("test event B").build();
    MuleMessage message3 = MuleMessage.builder().payload("test event C").build();
    MuleMessage message4 = MuleMessage.builder().payload("test event D").build();
    MuleMessage message5 = MuleMessage.builder().payload("test event E").build();

    List<MuleMessage> list = new ArrayList<>();
    list.add(message2);
    list.add(message3);
    MuleMessage messageCollection = MuleMessage.builder().payload(list).build();
    DefaultMuleEvent event2 = new DefaultMuleEvent(messageCollection, flow);
    DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint(event2, endpoint);
    event2.setFlowVariable("key2", "value2");

    List<MuleMessage> list2 = new ArrayList<>();
    list.add(message4);
    list.add(message5);
    MuleMessage messageCollection2 = MuleMessage.builder().payload(list2).build();
    DefaultMuleEvent event3 = new DefaultMuleEvent(messageCollection2, flow);
    DefaultMuleEventEndpointUtils.populateFieldsFromInboundEndpoint(event3, endpoint);
    event3.setFlowVariable("key3", "value3");

    List<MuleEvent> events = new ArrayList<>();
    events.add(event2);
    events.add(event3);

    MuleEvent result = resultsHandler.aggregateResults(events, event1);
    assertNotNull(result);
    assertEquals(2, ((List<MuleMessage>) result.getMessage().getPayload()).size());
    assertTrue(result.getMessage().getPayload() instanceof List<?>);
    assertEquals(messageCollection, ((List<MuleMessage>) result.getMessage().getPayload()).get(0));
    assertEquals(messageCollection2, ((List<MuleMessage>) result.getMessage().getPayload()).get(1));

    // Because a new MuleMessageCollection is created, propagate properties from
    // original event
    assertEquals("value1", result.getFlowVariable("key1"));
    assertNull(result.getFlowVariable("key2"));
    assertNull(result.getFlowVariable("key3"));

    // Root id
    assertEquals(event1.getMessage().getMessageRootId(), result.getMessage().getMessageRootId());
  }

}
