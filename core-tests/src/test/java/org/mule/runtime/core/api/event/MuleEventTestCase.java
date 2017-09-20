/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.event;

import static java.time.Duration.ofMillis;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.setCurrentEvent;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.MuleTestUtils.getTestFlow;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.api.security.DefaultMuleAuthentication;
import org.mule.runtime.core.api.security.DefaultMuleCredentials;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.security.DefaultSecurityContextFactory;
import org.mule.runtime.core.internal.util.SerializationUtils;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.transformer.simple.ByteArrayToObject;
import org.mule.runtime.core.privileged.transformer.simple.SerializableToByteArray;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


public class MuleEventTestCase extends AbstractMuleContextTestCase {

  private static String TIMEOUT_ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE = "Timeout on Mono blocking read";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void serialization() throws Exception {
    Transformer transformer = createSerializableToByteArrayTransformer();
    transformer.setMuleContext(muleContext);
    Serializable serialized = (Serializable) createSerializableToByteArrayTransformer().transform(testEvent());
    assertNotNull(serialized);
    ByteArrayToObject trans = new ByteArrayToObject();
    trans.setMuleContext(muleContext);
    PrivilegedEvent deserialized = (PrivilegedEvent) trans.transform(serialized);

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
    BaseEvent event = createEventToSerialize();

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
    PrivilegedEvent deserialized = (PrivilegedEvent) trans.transform(serialized);

    // Assert that deserialized event is not null
    assertNotNull(deserialized);

    // Assert that deserialized event has session with same id
    assertNotNull(deserialized.getSession());
  }

  private BaseEvent createEventToSerialize() throws Exception {
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
    PrivilegedEvent testEvent = this.<PrivilegedEvent.Builder>getEventBuilder()
        .message(of(new ByteArrayInputStream(payload.toString().getBytes()))).build();
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
    BaseEvent event = eventBuilder()
        .message(of("whatever"))
        .addVariable("test", "val")
        .build();
    event.getVariables().keySet().add("other");
  }

  public void testFlowVarNamesRemoveMutable() throws Exception {
    BaseEvent event = eventBuilder()
        .message(of("whatever"))
        .addVariable("test", "val")
        .build();
    event = BaseEvent.builder(event).addVariable("test", "val").build();
    event.getVariables().keySet().remove("test");
    assertNull(event.getVariables().get("test").getValue());
  }

  @Test
  public void testFlowVarsNotShared() throws Exception {
    BaseEvent event = eventBuilder()
        .message(of("whatever"))
        .addVariable("foo", "bar")
        .build();
    event = BaseEvent.builder(event).addVariable("foo", "bar").build();

    BaseEvent copy = BaseEvent.builder(event).build();

    copy = BaseEvent.builder(copy).addVariable("foo", "bar2").build();

    assertEquals("bar", event.getVariables().get("foo").getValue());

    assertEquals("bar2", copy.getVariables().get("foo").getValue());
  }

  @Test
  public void eventContextSerializationNoPipelinePublisherLost() throws Exception {

    BaseEvent result = testEvent();
    BaseEvent before = testEvent();

    // Remove Flow to simulate deserialization when Flow is not available
    muleContext.getRegistry().unregisterObject(APPLE_FLOW);

    BaseEvent after =
        (BaseEvent) SerializationUtils.deserialize(org.apache.commons.lang3.SerializationUtils.serialize(before),
                                                   muleContext);

    ((BaseEventContext) after.getContext()).success(result);

    assertThat(before.getContext().getId(), equalTo(after.getContext().getId()));

    // Publisher is not conserved after serialization due to null FlowConstruct so attempting to obtain result via before event
    // fails with timeout.
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage(startsWith(TIMEOUT_ILLEGAL_ARGUMENT_EXCEPTION_MESSAGE));
    from(((BaseEventContext) before.getContext()).getResponsePublisher()).block(ofMillis(BLOCK_TIMEOUT));
  }

  @Test
  public void eventContextSerializationEventContextGarbageCollected() throws Exception {

    Flow flow = getTestFlow(muleContext);
    BaseEvent before = this.<PrivilegedEvent.Builder>getEventBuilder().message(of(null)).flow(flow).build();
    String beforeId = before.getContext().getId();

    byte[] bytes = org.apache.commons.lang3.SerializationUtils.serialize(before);
    before = null;
    System.gc();

    // The event is never deserialized but it is cleaned up by garbage collection due to WeakReference
    assertThat(flow.getSerializationEventContextCache().get(beforeId), is(nullValue()));
  }

  @Test
  public void eventContextSerializationPublisherConserved() throws Exception {
    BaseEvent result = testEvent();
    BaseEvent before = this.<PrivilegedEvent.Builder>getEventBuilder().message(of(null)).flow(getTestFlow(muleContext)).build();

    BaseEvent after =
        (BaseEvent) SerializationUtils.deserialize(org.apache.commons.lang3.SerializationUtils.serialize(before),
                                                   muleContext);

    ((BaseEventContext) after.getContext()).success(result);

    assertThat(before.getContext().getId(), equalTo(after.getContext().getId()));

    // Publisher is conserved after serialization so attempting to obtain result via before event is successful.
    assertThat(from(((BaseEventContext) before.getContext()).getResponsePublisher()).block(), equalTo(result));

    // Cache entry is removed on deserialization
    assertThat(((Pipeline) ((InternalEvent) before).getFlowConstruct()).getSerializationEventContextCache()
        .get(before.getContext().getId()), is(nullValue()));

  }

  @Test
  public void securityContextCopy() throws Exception {
    SecurityContext securityContext = mock(SecurityContext.class);
    BaseEvent event = BaseEvent.builder(testEvent()).securityContext(securityContext).build();

    BaseEvent eventCopy = BaseEvent.builder(event).message(Message.of("copy")).build();

    assertThat(securityContext, sameInstance(eventCopy.getSecurityContext()));
  }

  @Test
  public void securityContextSerialization() throws Exception {
    Transformer transformer = createSerializableToByteArrayTransformer();
    transformer.setMuleContext(muleContext);

    BaseEvent event = BaseEvent.builder(testEvent()).securityContext(createTestAuthentication()).build();

    Serializable serialized = (Serializable) createSerializableToByteArrayTransformer().transform(event);
    assertNotNull(serialized);
    ByteArrayToObject trans = new ByteArrayToObject();
    trans.setMuleContext(muleContext);
    BaseEvent deserialized = (BaseEvent) trans.transform(serialized);

    assertThat(deserialized.getSecurityContext().getAuthentication().getPrincipal(),
               is(event.getSecurityContext().getAuthentication().getPrincipal()));
    assertThat(deserialized.getSecurityContext().getAuthentication().getProperties().get("key1"),
               is(event.getSecurityContext().getAuthentication().getProperties().get("key1")));
    assertThat(deserialized.getSecurityContext().getAuthentication().getCredentials(),
               is(event.getSecurityContext().getAuthentication().getCredentials()));
  }

  private SecurityContext createTestAuthentication() {
    Authentication auth = new DefaultMuleAuthentication(new DefaultMuleCredentials("dan", new char[] {'d', 'f'}));
    SecurityContext securityContext =
        new DefaultSecurityContextFactory().create(auth.setProperties(singletonMap("key1", "value1")));
    return securityContext;
  }

  private static class TestEventTransformer extends AbstractTransformer {

    @Override
    public Object doTransform(Object src, Charset encoding) throws TransformerException {
      return "Transformed Test Data";
    }
  }

}
