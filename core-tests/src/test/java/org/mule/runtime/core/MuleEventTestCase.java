/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.Event.setCurrentEvent;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.transformer.simple.ByteArrayToObject;
import org.mule.runtime.core.transformer.simple.SerializableToByteArray;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;


public class MuleEventTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testEventSerialization() throws Exception {
    setCurrentEvent(testEvent());

    Transformer transformer = createSerializableToByteArrayTransformer();
    transformer.setMuleContext(muleContext);
    Serializable serialized = (Serializable) createSerializableToByteArrayTransformer().transform(testEvent());
    assertNotNull(serialized);
    ByteArrayToObject trans = new ByteArrayToObject();
    trans.setMuleContext(muleContext);
    Event deserialized = (Event) trans.transform(serialized);

    // Assert that deserialized event is not null
    assertNotNull(deserialized);

    // Assert that deserialized event has session with same id
    assertNotNull(deserialized.getSession());
  }

  private Transformer createSerializableToByteArrayTransformer() {
    Transformer transformer = new SerializableToByteArray();
    transformer.setMuleContext(muleContext);

    return transformer;
  }

  @Test
  public void testEventSerializationRestart() throws Exception {
    // Create and register artifacts
    Event event = createEventToSerialize();
    muleContext.start();

    // Serialize
    Serializable serialized = (Serializable) createSerializableToByteArrayTransformer().transform(event);
    assertNotNull(serialized);

    // Simulate mule cold restart
    muleContext.dispose();
    muleContext = createMuleContext();
    muleContext.start();
    ByteArrayToObject trans = new ByteArrayToObject();
    trans.setMuleContext(muleContext);

    // Recreate and register artifacts (this would happen if using any kind of static config e.g. XML)
    createAndRegisterTransformersEndpointBuilderService();

    // Deserialize
    Event deserialized = (Event) trans.transform(serialized);

    // Assert that deserialized event is not null
    assertNotNull(deserialized);

    // Assert that deserialized event has session with same id
    assertNotNull(deserialized.getSession());
  }

  private Event createEventToSerialize() throws Exception {
    createAndRegisterTransformersEndpointBuilderService();
    return testEvent();
  }

  @Test
  public void testMuleEventSerializationWithRawPayload() throws Exception {
    StringBuilder payload = new StringBuilder();
    // to reproduce issue we must try to serialize something with a payload bigger than 1020 bytes
    for (int i = 0; i < 108; i++) {
      payload.append("1234567890");
    }
    Event testEvent = eventBuilder()
        .message(InternalMessage.of(new ByteArrayInputStream(payload.toString().getBytes())))
        .exchangePattern(REQUEST_RESPONSE)
        .build();
    setCurrentEvent(testEvent);
    byte[] serializedEvent = muleContext.getObjectSerializer().getExternalProtocol().serialize(testEvent);
    testEvent = muleContext.getObjectSerializer().getExternalProtocol().deserialize(serializedEvent);

    assertArrayEquals((byte[]) testEvent.getMessage().getPayload().getValue(), payload.toString().getBytes());
  }

  private void createAndRegisterTransformersEndpointBuilderService() throws Exception {
    Transformer trans1 = new TestEventTransformer();
    trans1.setName("OptimusPrime");
    muleContext.getRegistry().registerTransformer(trans1);

    Transformer trans2 = new TestEventTransformer();
    trans2.setName("Bumblebee");
    muleContext.getRegistry().registerTransformer(trans2);

    List<Transformer> transformers = new ArrayList<>();
    transformers.add(trans1);
    transformers.add(trans2);
  }


  @Test(expected = UnsupportedOperationException.class)
  public void testFlowVarNamesAddImmutable() throws Exception {
    Event event = eventBuilder()
        .message(InternalMessage.of("whatever"))
        .addVariable("test", "val")
        .build();
    event.getVariableNames().add("other");
  }

  public void testFlowVarNamesRemoveMutable() throws Exception {
    Event event = eventBuilder()
        .message(InternalMessage.of("whatever"))
        .addVariable("test", "val")
        .build();
    event = Event.builder(event).addVariable("test", "val").build();
    event.getVariableNames().remove("test");
    assertNull(event.getVariable("test").getValue());
  }

  @Test
  public void testFlowVarsNotShared() throws Exception {
    Event event = eventBuilder()
        .message(InternalMessage.of("whatever"))
        .addVariable("foo", "bar")
        .build();
    event = Event.builder(event).addVariable("foo", "bar").build();

    Event copy = Event.builder(event).build();

    copy = Event.builder(copy).addVariable("foo", "bar2").build();

    assertEquals("bar", event.getVariable("foo").getValue());

    assertEquals("bar2", copy.getVariable("foo").getValue());
  }

  @Test(expected = NoSuchElementException.class)
  public void testGetFlowVarNonexistent() throws Exception {
    testEvent().getVariable("foo").getValue();
  }

  @Test(expected = NoSuchElementException.class)
  public void testGetFlowVarDataTypeNonexistent() throws Exception {
    testEvent().getVariable("foo").getDataType();
  }

  private static class TestEventTransformer extends AbstractTransformer {

    @Override
    public Object doTransform(Object src, Charset encoding) throws TransformerException {
      return "Transformed Test Data";
    }
  }

}
