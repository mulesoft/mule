/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.session;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_SESSION_PROPERTY;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.serialization.ObjectSerializer;
import org.mule.runtime.core.api.serialization.SerializationProtocol;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SessionPropertiesTestCase extends AbstractMuleContextTestCase {

  private Flow flow;
  private EventContext context;
  private Scheduler scheduler;

  @Before
  public void before() throws Exception {
    flow = getTestFlow(muleContext);
    context = DefaultEventContext.create(flow, TEST_CONNECTOR);
    scheduler = muleContext.getSchedulerService().computationScheduler();
  }

  @After
  public void after() {
    scheduler.shutdownNow();
  }

  /**
   * MuleSession is not copied when async intercepting processor is used
   */
  @Test
  public void serializationSessionPropertyPropagation() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("data").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flow).build();

    event.getSession().setProperty("key", "value");

    ObjectSerializer serializer = muleContext.getObjectSerializer();
    SerializationProtocol serializationProtocol = serializer.getExternalProtocol();
    Event deserializedEvent = serializationProtocol.deserialize(serializationProtocol.serialize(event));

    // Event and session are both copied
    assertNotSame(deserializedEvent, event);
    assertNotSame(deserializedEvent, event);
    assertNotSame(deserializedEvent.getSession(), event.getSession());
    assertFalse(deserializedEvent.getSession().equals(event.getSession()));

    // Session properties available before serialization are available after too
    assertEquals(1, deserializedEvent.getSession().getPropertyNamesAsSet().size());
    assertEquals("value", deserializedEvent.getSession().getProperty("key"));

    // Session properties set after deserialization are not available in message
    // processor
    // before serialization
    deserializedEvent.getSession().setProperty("newKey", "newValue");
    assertEquals(2, deserializedEvent.getSession().getPropertyNamesAsSet().size());
    assertEquals("newValue", deserializedEvent.getSession().getProperty("newKey"));
    assertEquals(1, event.getSession().getPropertyNamesAsSet().size());
    assertNull(event.getSession().getProperty("newKey"));
  }

  /**
   * Serialization of a MuleSession with session properties fails (no warning is given)
   */
  @Test
  public void defaultSessionHandlerSessionPropertyPropagation() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("data").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flow).build();

    event.getSession().setProperty("key", "value");

    // Serialize and deserialize session using default session handler
    message = new SerializeAndEncodeSessionHandler().storeSessionInfoToMessage(event.getSession(), message, muleContext);
    message = InternalMessage.builder(message)
        .addInboundProperty(MULE_SESSION_PROPERTY, message.getOutboundProperty(MULE_SESSION_PROPERTY)).build();
    MuleSession newSession = new SerializeAndEncodeSessionHandler().retrieveSessionInfoFromMessage(message, muleContext);

    // Session after deserialization is a new instance that does not equal old
    // instance
    assertNotSame(newSession, event.getSession());
    assertFalse(newSession.equals(event.getSession()));

    // Session properties available before serialization are available after too
    assertEquals(1, newSession.getPropertyNamesAsSet().size());
    assertEquals("value", newSession.getProperty("key"));

    // Session properties set after deserialization are not available in message
    // processor
    // before serialization
    newSession.setProperty("newKey", "newValue");
    assertEquals(2, newSession.getPropertyNamesAsSet().size());
    assertEquals("newValue", newSession.getProperty("newKey"));
    assertEquals(1, event.getSession().getPropertyNamesAsSet().size());
    assertNull(event.getSession().getProperty("newKey"));
  }

  /**
   * Serialization of a MuleSession with session properties serializes only serializable properties
   */
  @Test
  public void serializationNonSerializableSessionPropertyPropagation() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("data").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flow).build();

    Object nonSerializable = new Object();
    event.getSession().setProperty("key", nonSerializable);
    event.getSession().setProperty("key2", "value2");

    ObjectSerializer serializer = muleContext.getObjectSerializer();
    SerializationProtocol serializationProtocol = serializer.getExternalProtocol();
    Event deserialized = serializationProtocol.deserialize(serializationProtocol.serialize(event));

    // Serialization no longer fails as in 3.1.x/3.2.x

    assertEquals(nonSerializable, event.getSession().getProperty("key"));
    assertEquals("value2", event.getSession().getProperty("key2"));

    assertNotSame(deserialized, event);
    assertNull(deserialized.getSession().getProperty("key"));
    assertEquals("value2", deserialized.getSession().getProperty("key2"));
  }

  /**
   * Serialization of a MuleSession with session properties to message using SessionHandler serializes only serializable
   * properties
   */
  @Test
  public void defaultSessionHandlerNonSerializableSessionPropertyPropagation() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("data").build();
    Event event = Event.builder(context).message(message).exchangePattern(ONE_WAY).flow(flow).build();

    Object nonSerializable = new Object();
    event.getSession().setProperty("key", nonSerializable);
    event.getSession().setProperty("key2", "value2");

    // Serialize and deserialize session using default session handler
    message = new SerializeAndEncodeSessionHandler().storeSessionInfoToMessage(event.getSession(), message, muleContext);
    message = InternalMessage.builder(message)
        .addInboundProperty(MULE_SESSION_PROPERTY, message.getOutboundProperty(MULE_SESSION_PROPERTY)).build();
    MuleSession newSession = new SerializeAndEncodeSessionHandler().retrieveSessionInfoFromMessage(message, muleContext);

    // Session after deserialization is a new instance that does not equal old
    // instance
    assertThat(newSession, not(sameInstance(event.getSession())));
    assertThat(newSession, not(event.getSession()));
    assertEquals(nonSerializable, event.getSession().getProperty("key"));
    assertEquals("value2", event.getSession().getProperty("key2"));

    // Non-serilizable session properties available before serialization are not
    // available after too
    assertEquals(1, newSession.getPropertyNamesAsSet().size());
    assertNull(newSession.getProperty("key"));
    assertEquals("value2", newSession.getProperty("key2"));
  }

  /**
   * When invoking a Flow directly session properties are preserved
   */
  @Test
  public void processFlowSessionPropertyPropagation() throws Exception {
    InternalMessage message = InternalMessage.builder().payload("data").build();
    Event event = Event.builder(context).message(message).exchangePattern(REQUEST_RESPONSE).flow(flow).build();

    SensingNullMessageProcessor flowListener = new SensingNullMessageProcessor();
    Flow flow = new Flow("flow", muleContext);
    flow.setMessageProcessors(Collections.<Processor>singletonList(flowListener));
    flow.initialise();
    flow.start();

    Object nonSerializable = new Object();
    event.getSession().setProperty("key", "value");
    event.getSession().setProperty("key2", nonSerializable);

    flow.process(event);

    flowListener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);
    Event processedEvent = flowListener.event;

    // Event is copied, but session isn't
    assertNotSame(processedEvent, event);
    assertEquals(processedEvent.getMessage(), event.getMessage());
    assertSame(processedEvent.getSession(), event.getSession());

    // Session properties available before new flow are available after too
    assertEquals(2, processedEvent.getSession().getPropertyNamesAsSet().size());
    assertEquals("value", processedEvent.getSession().getProperty("key"));
    assertEquals(nonSerializable, processedEvent.getSession().getProperty("key2"));

    // Session properties set after new flow are available in message processor
    // before new flow
    processedEvent.getSession().setProperty("newKey", "newValue");
    assertEquals(3, processedEvent.getSession().getPropertyNamesAsSet().size());
    assertEquals("newValue", processedEvent.getSession().getProperty("newKey"));
    assertEquals(3, event.getSession().getPropertyNamesAsSet().size());
    assertEquals("newValue", event.getSession().getProperty("newKey"));

    flow.stop();
    flow.dispose();
  }

}
