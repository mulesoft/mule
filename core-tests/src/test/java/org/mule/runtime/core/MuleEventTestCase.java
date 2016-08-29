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
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.routing.filters.PayloadTypeFilter;
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

  private static final String TEST_PAYLOAD = "anyValuePayload";

  @Test
  public void testEventSerialization() throws Exception {
    MuleEvent event = getTestEvent("payload");
    setCurrentEvent(event);

    Transformer transformer = createSerializableToByteArrayTransformer();
    transformer.setMuleContext(muleContext);
    Serializable serialized = (Serializable) createSerializableToByteArrayTransformer().transform(event);
    assertNotNull(serialized);
    ByteArrayToObject trans = new ByteArrayToObject();
    trans.setMuleContext(muleContext);
    MuleEvent deserialized = (MuleEvent) trans.transform(serialized);

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
    MuleEvent event = createEventToSerialize();
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
    MuleEvent deserialized = (MuleEvent) trans.transform(serialized);

    // Assert that deserialized event is not null
    assertNotNull(deserialized);

    // Assert that deserialized event has session with same id
    assertNotNull(deserialized.getSession());
  }

  private MuleEvent createEventToSerialize() throws Exception {
    createAndRegisterTransformersEndpointBuilderService();
    Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("appleService");
    return getTestEvent(TEST_PAYLOAD);
  }

  @Test
  public void testMuleEventSerializationWithRawPayload() throws Exception {
    StringBuilder payload = new StringBuilder();
    // to reproduce issue we must try to serialize something with a payload bigger than 1020 bytes
    for (int i = 0; i < 108; i++) {
      payload.append("1234567890");
    }
    MuleEvent testEvent = getTestEvent(new ByteArrayInputStream(payload.toString().getBytes()));
    setCurrentEvent(testEvent);
    byte[] serializedEvent = muleContext.getObjectSerializer().serialize(testEvent);
    testEvent = muleContext.getObjectSerializer().deserialize(serializedEvent);

    assertArrayEquals((byte[]) testEvent.getMessage().getPayload(), payload.toString().getBytes());
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

    Filter filter = new PayloadTypeFilter(Object.class);
    getTestFlow();
  }


  @Test(expected = UnsupportedOperationException.class)
  public void testFlowVarNamesAddImmutable() throws Exception {
    MuleEvent event = getTestEvent("whatever");
    event.setFlowVariable("test", "val");
    event.getFlowVariableNames().add("other");
  }

  public void testFlowVarNamesRemoveMutable() throws Exception {
    MuleEvent event = getTestEvent("whatever");
    event.setFlowVariable("test", "val");
    event.getFlowVariableNames().remove("test");
    assertNull(event.getFlowVariable("test"));
  }

  @Test
  public void testFlowVarsNotShared() throws Exception {
    MuleEvent event = getTestEvent("whatever");
    event.setFlowVariable("foo", "bar");

    MuleEvent copy = MuleEvent.builder(event).build();

    copy.setFlowVariable("foo", "bar2");

    assertEquals("bar", event.getFlowVariable("foo"));

    assertEquals("bar2", copy.getFlowVariable("foo"));
  }

  @Test
  public void testFlowVarsShared() throws Exception {
    MuleEvent event = getTestEvent("whatever");
    event.setFlowVariable("foo", "bar");

    MuleEvent copy = new DefaultMuleEvent(event.getMessage(), event, false);

    copy.setFlowVariable("foo", "bar2");

    assertEquals("bar2", event.getFlowVariable("foo"));

    assertEquals("bar2", copy.getFlowVariable("foo"));
  }

  @Test(expected = NoSuchElementException.class)
  public void testGetFlowVarNonexistent() throws Exception {
    MuleEvent event = getTestEvent("whatever");
    event.getFlowVariable("foo");
  }

  @Test(expected = NoSuchElementException.class)
  public void testGetFlowVarDataTypeNonexistent() throws Exception {
    MuleEvent event = getTestEvent("whatever");
    event.getFlowVariableDataType("foo");
  }

  private static class TestEventTransformer extends AbstractTransformer {

    @Override
    public Object doTransform(Object src, Charset encoding) throws TransformerException {
      return "Transformed Test Data";
    }
  }

}
