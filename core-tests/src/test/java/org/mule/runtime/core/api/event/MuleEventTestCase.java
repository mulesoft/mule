/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.event;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.internal.context.DefaultMuleContext.currentMuleContext;
import static org.mule.test.allure.AllureConstants.MuleEvent.MULE_EVENT;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.DefaultMuleAuthentication;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.security.DefaultMuleCredentials;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.security.DefaultSecurityContextFactory;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.transformer.TransformersRegistry;
import org.mule.runtime.core.privileged.transformer.simple.ByteArrayToObject;
import org.mule.runtime.core.privileged.transformer.simple.SerializableToByteArray;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


@Feature(MULE_EVENT)
public class MuleEventTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() {
    currentMuleContext.set(muleContext);
  }

  @After
  public void teardown() {
    currentMuleContext.set(null);
  }

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
    CoreEvent event = createEventToSerialize();

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

  private CoreEvent createEventToSerialize() throws Exception {
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
    byte[] serializedEvent = muleContext.getObjectSerializer().getExternalProtocol().serialize(testEvent);
    testEvent = muleContext.getObjectSerializer().getExternalProtocol().deserialize(serializedEvent);

    assertArrayEquals((byte[]) testEvent.getMessage().getPayload().getValue(), payload.toString().getBytes());
  }

  private void createAndRegisterTransformersEndpointBuilderService() throws Exception {
    TransformersRegistry transformersRegistry =
        ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(TransformersRegistry.class);

    Transformer trans1 = new TestEventTransformer();
    trans1.setName("OptimusPrime");
    transformersRegistry.registerTransformer(trans1);

    Transformer trans2 = new TestEventTransformer();
    trans2.setName("Bumblebee");
    transformersRegistry.registerTransformer(trans2);

    List<Transformer> transformers = new ArrayList<>();
    transformers.add(trans1);
    transformers.add(trans2);
  }

  @Test
  public void testFlowVarNamesAddImmutable() throws Exception {
    CoreEvent event = getEventBuilder()
        .message(of("whatever"))
        .addVariable("test", "val")
        .build();
    expectedException.expect(UnsupportedOperationException.class);
    event.getVariables().keySet().add("other");
  }

  @Test
  public void testFlowVarNamesRemoveImmutable() throws Exception {
    CoreEvent event = getEventBuilder()
        .message(of("whatever"))
        .addVariable("test", "val")
        .build();
    event = CoreEvent.builder(event).addVariable("test", "val").build();
    expectedException.expect(UnsupportedOperationException.class);
    event.getVariables().keySet().remove("test");
  }

  @Test
  public void testFlowVarsNotShared() throws Exception {
    CoreEvent event = getEventBuilder()
        .message(of("whatever"))
        .addVariable("foo", "bar")
        .build();
    event = CoreEvent.builder(event).addVariable("foo", "bar").build();

    CoreEvent copy = CoreEvent.builder(event).build();

    copy = CoreEvent.builder(copy).addVariable("foo", "bar2").build();

    assertEquals("bar", event.getVariables().get("foo").getValue());

    assertEquals("bar2", copy.getVariables().get("foo").getValue());
  }

  @Test
  @Description("Test that a perfromance optimization to avoid recreating the variables map is applied")
  public void varsOverridenFromAnotherEvent() throws MuleException {
    CoreEvent baseEventWithVars = getEventBuilder()
        .message(of("whatever"))
        .addVariable("foo", "bar")
        .build();
    CoreEvent baseEventNoVars = getEventBuilder()
        .message(of("whatever"))
        .build();

    final Map<String, TypedValue<?>> baseEventVars = baseEventWithVars.getVariables();

    final PrivilegedEvent newEvent = PrivilegedEvent.builder(baseEventNoVars).variablesTyped(baseEventVars).build();

    assertThat(newEvent.getVariables(), sameInstance(baseEventVars));
  }

  @Test
  @Description("Test that a performance optimization to avoid recreating the variables map is applied")
  public void varsOverridenFromAnotherEventNotEmpty() throws MuleException {
    CoreEvent baseEventWithVars = getEventBuilder()
        .message(of("whatever"))
        .addVariable("foo", "bar")
        .build();
    CoreEvent baseEventWithOtherVars = getEventBuilder()
        .message(of("whatever"))
        .addVariable("baz", "qux")
        .build();

    final Map<String, TypedValue<?>> baseEventVars = baseEventWithVars.getVariables();

    final PrivilegedEvent newEvent = PrivilegedEvent.builder(baseEventWithOtherVars).variablesTyped(baseEventVars).build();

    assertThat(newEvent.getVariables(), sameInstance(baseEventVars));
  }

  @Test
  public void varsCleared() throws MuleException {
    CoreEvent baseEventWithVars = getEventBuilder()
        .message(of("whatever"))
        .addVariable("foo", "bar")
        .build();

    final CoreEvent newEvent = CoreEvent.builder(baseEventWithVars).clearVariables().build();

    assertThat(newEvent.getVariables().isEmpty(), is(true));
  }

  @Test
  public void varsClearedAndAdded() throws MuleException {
    CoreEvent baseEventWithVars = getEventBuilder()
        .message(of("whatever"))
        .addVariable("foo", "bar")
        .build();

    String key = "survivor";
    String value = "Tom Hanks";
    final CoreEvent newEvent = CoreEvent.builder(baseEventWithVars)
        .clearVariables()
        .addVariable(key, value)
        .build();

    assertThat(newEvent.getVariables().size(), is(1));

    TypedValue<?> actual = newEvent.getVariables().get(key);
    assertThat(actual.getValue(), is(value));
  }

  @Test
  public void setParameters() throws Exception {
    Map<String, Object> parameters = testParameterValues();
    CoreEvent event = getEventBuilder()
        .message(of(""))
        .parameters(parameters)
        .build();

    assertTypedValueMap(parameters, event.getParameters());
  }

  @Test
  public void modifyParameters() throws Exception {
    Map<String, Object> parameters = testParameterValues();
    CoreEvent event = getEventBuilder()
        .message(of(""))
        .parameters(parameters)
        .build();

    Map<String, Object> mutatedParams = new HashMap<>(parameters);
    mutatedParams.put("I'm", "a new entry");

    CoreEvent eventCopy = CoreEvent.builder(event)
        .parameters(mutatedParams)
        .build();

    assertThat(event.getParameters(), is(not(sameInstance(eventCopy.getParameters()))));
    assertTypedValueMap(parameters, event.getParameters());
    assertTypedValueMap(mutatedParams, eventCopy.getParameters());
  }

  @Test
  public void clearParameters() throws Exception {
    Map<String, Object> parameters = testParameterValues();
    CoreEvent event = getEventBuilder()
        .message(of(""))
        .parameters(parameters)
        .build();

    assertTypedValueMap(parameters, event.getParameters());
    CoreEvent eventCopy = CoreEvent.builder(event)
        .clearParameters()
        .build();

    assertTypedValueMap(parameters, event.getParameters());
    assertThat(eventCopy.getParameters().isEmpty(), is(true));
  }

  @Test
  public void parametersKeptInCopies() throws Exception {
    Map<String, Object> parameters = testParameterValues();
    CoreEvent event = getEventBuilder()
        .message(of(""))
        .parameters(parameters)
        .build();

    assertTypedValueMap(parameters, event.getParameters());
    CoreEvent eventCopy = CoreEvent.builder(event)
        .message(Message.of("I'm a copy"))
        .build();

    assertThat(event.getParameters(), is(sameInstance(eventCopy.getParameters())));
  }

  @Test
  public void parametersAreImmutable() throws Exception {
    CoreEvent event = getEventBuilder()
        .message(of(""))
        .parameters(testParameterValues())
        .build();

    expectedException.expect(UnsupportedOperationException.class);
    event.getParameters().put("a new param", new TypedValue<>("value", STRING));
  }

  @Test
  public void typedValueParametersPreserved() throws Exception {
    Map<String, TypedValue<?>> typedValueMap = new HashMap<>();
    testParameterValues().forEach((k, v) -> typedValueMap.put(k, new TypedValue<>(v, STRING)));

    CoreEvent event = getEventBuilder()
        .message(of(""))
        .parameters(typedValueMap)
        .build();

    event.getParameters().forEach((k, v) -> assertThat(v, is(sameInstance(typedValueMap.get(k)))));
  }

  private Map<String, Object> testParameterValues() {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("param1", "value1");
    parameters.put("param2", "value2");

    return parameters;
  }

  private void assertTypedValueMap(Map<String, Object> expected, Map<String, TypedValue<?>> actual) {
    assertThat(actual.size(), is(expected.size()));
    expected.forEach((k, v) -> {
      TypedValue<?> value = actual.get(k);
      assertThat(value, is(notNullValue()));
      assertThat(value.getValue(), equalTo(v));
    });
  }


  @Test
  public void securityContextCopy() throws Exception {
    SecurityContext securityContext = mock(SecurityContext.class);
    CoreEvent event = CoreEvent.builder(testEvent()).securityContext(securityContext).build();

    CoreEvent eventCopy = CoreEvent.builder(event).message(Message.of("copy")).build();

    assertThat(securityContext, sameInstance(eventCopy.getSecurityContext()));
  }

  @Test
  @Issue("MULE-18157")
  public void securityContextNull() throws Exception {
    CoreEvent event = CoreEvent.builder(testEvent()).securityContext(null).build();

    CoreEvent eventCopy = CoreEvent.builder(event).message(Message.of("copy")).securityContext(null).build();

    assertThat(eventCopy.getMessage().getPayload().getValue(), is("copy"));
  }

  @Test
  public void securityContextSerialization() throws Exception {
    Transformer transformer = createSerializableToByteArrayTransformer();
    transformer.setMuleContext(muleContext);

    CoreEvent event = CoreEvent.builder(testEvent()).securityContext(createTestAuthentication()).build();

    Serializable serialized = (Serializable) createSerializableToByteArrayTransformer().transform(event);
    assertNotNull(serialized);
    ByteArrayToObject trans = new ByteArrayToObject();
    trans.setMuleContext(muleContext);
    CoreEvent deserialized = (CoreEvent) trans.transform(serialized);

    assertThat(deserialized.getSecurityContext().getAuthentication().getPrincipal(),
               is(event.getSecurityContext().getAuthentication().getPrincipal()));
    assertThat(deserialized.getSecurityContext().getAuthentication().getProperties().get("key1"),
               is(event.getSecurityContext().getAuthentication().getProperties().get("key1")));
    assertThat(deserialized.getSecurityContext().getAuthentication().getCredentials(),
               is(event.getSecurityContext().getAuthentication().getCredentials()));
  }

  @Test
  @Description("Validates that the correlation IDs are unique")
  @Issue("MULE-17926")
  public void uniqueCorrelationIDs() throws MuleException {
    CoreEvent firstEvent = getEventBuilder().message(of("first")).build();
    CoreEvent secondEvent = getEventBuilder().message(of("second")).build();

    assertThat("Duplicated correlationID", firstEvent.getContext().getCorrelationId(),
               not(is(secondEvent.getContext().getCorrelationId())));
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
