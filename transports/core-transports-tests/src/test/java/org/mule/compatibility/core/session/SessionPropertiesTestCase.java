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

import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MessageContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.serialization.ObjectSerializer;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.processor.AsyncInterceptingMessageProcessor;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

public class SessionPropertiesTestCase extends AbstractMuleContextTestCase {

  private Flow flow;
  private MessageContext context;

  @Before
  public void before() throws Exception {
    flow = getTestFlow();
    context = DefaultMessageContext.create(flow, TEST_CONNECTOR);
  }

  /**
   * MuleSession is not copied when async intercepting processor is used
   */
  @Test
  public void asyncInterceptingProcessorSessionPropertyPropagation() throws Exception {
    AsyncInterceptingMessageProcessor async =
        new AsyncInterceptingMessageProcessor(muleContext.getDefaultThreadingProfile(), "async", 0);
    SensingNullMessageProcessor asyncListener = new SensingNullMessageProcessor();
    async.setListener(asyncListener);
    async.start();

    MuleMessage message = MuleMessage.builder().payload("data").build();
    MuleEvent event = MuleEvent.builder(context).message(message).exchangePattern(ONE_WAY).flow(flow).build();

    event.getSession().setProperty("key", "value");

    async.process(event);
    asyncListener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);

    MuleEvent asyncEvent = asyncListener.event;

    // Event is copied, but session isn't
    assertNotSame(asyncEvent, event);
    assertNotSame(asyncEvent, event);
    assertNotSame(asyncEvent.getSession(), event.getSession());

    // Session properties available before async are available after too
    assertEquals(1, asyncEvent.getSession().getPropertyNamesAsSet().size());
    assertEquals("value", asyncEvent.getSession().getProperty("key"));

    // Session properties set after async are available in message processor
    // before async
    asyncEvent.getSession().setProperty("newKey", "newValue");
    assertEquals(2, asyncEvent.getSession().getPropertyNamesAsSet().size());
    assertEquals("newValue", asyncEvent.getSession().getProperty("newKey"));
    assertEquals(1, event.getSession().getPropertyNamesAsSet().size());
    assertNull(event.getSession().getProperty("newKey"));

    async.stop();
  }

  /**
   * MuleSession is not copied when async intercepting processor is used
   */
  @Test
  public void serializationSessionPropertyPropagation() throws Exception {
    MuleMessage message = MuleMessage.builder().payload("data").build();
    MuleEvent event = MuleEvent.builder(context).message(message).exchangePattern(ONE_WAY).flow(flow).build();

    event.getSession().setProperty("key", "value");

    ObjectSerializer serializer = muleContext.getObjectSerializer();
    MuleEvent deserializedEvent = serializer.deserialize(serializer.serialize(event));

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
    MuleMessage message = MuleMessage.builder().payload("data").build();
    MuleEvent event = MuleEvent.builder(context).message(message).exchangePattern(ONE_WAY).flow(flow).build();

    event.getSession().setProperty("key", "value");

    // Serialize and deserialize session using default session handler
    message = new SerializeAndEncodeSessionHandler().storeSessionInfoToMessage(event.getSession(), message, muleContext);
    message = MuleMessage.builder(message)
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
    MuleMessage message = MuleMessage.builder().payload("data").build();
    MuleEvent event = MuleEvent.builder(context).message(message).exchangePattern(ONE_WAY).flow(flow).build();

    Object nonSerializable = new Object();
    event.getSession().setProperty("key", nonSerializable);
    event.getSession().setProperty("key2", "value2");

    ObjectSerializer serializer = muleContext.getObjectSerializer();
    MuleEvent deserialized = serializer.deserialize(serializer.serialize(event));

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
    MuleMessage message = MuleMessage.builder().payload("data").build();
    MuleEvent event = MuleEvent.builder(context).message(message).exchangePattern(ONE_WAY).flow(flow).build();

    Object nonSerializable = new Object();
    event.getSession().setProperty("key", nonSerializable);
    event.getSession().setProperty("key2", "value2");

    // Serialize and deserialize session using default session handler
    message = new SerializeAndEncodeSessionHandler().storeSessionInfoToMessage(event.getSession(), message, muleContext);
    message = MuleMessage.builder(message)
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
    MuleMessage message = MuleMessage.builder().payload("data").build();
    MuleEvent event = MuleEvent.builder(context).message(message).exchangePattern(REQUEST_RESPONSE).flow(flow).build();

    SensingNullMessageProcessor flowListener = new SensingNullMessageProcessor();
    Flow flow = new Flow("flow", muleContext);
    flow.setMessageProcessors(Collections.<MessageProcessor>singletonList(flowListener));
    flow.initialise();
    flow.start();

    Object nonSerializable = new Object();
    event.getSession().setProperty("key", "value");
    event.getSession().setProperty("key2", nonSerializable);

    flow.process(event);

    flowListener.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);
    MuleEvent processedEvent = flowListener.event;

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
