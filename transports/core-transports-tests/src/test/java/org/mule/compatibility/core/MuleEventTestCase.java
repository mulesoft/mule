/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.security.Credentials;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.routing.MessageFilter;
import org.mule.runtime.core.routing.filters.PayloadTypeFilter;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.transformer.simple.ByteArrayToObject;
import org.mule.runtime.core.transformer.simple.SerializableToByteArray;
import org.mule.tck.MuleEndpointTestUtils;
import org.mule.tck.junit4.AbstractMuleContextEndpointTestCase;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;


public class MuleEventTestCase extends AbstractMuleContextEndpointTestCase {

  private static final String TEST_PAYLOAD = "anyValuePayload";

  /*
   * See http://mule.mulesoft.org/jira/browse/MULE-384 for details.
   */
  @Test
  public void testNoPasswordNoNullPointerException() throws Exception {
    // provide the username, but not the password, as is the case for SMTP
    // cannot set SMTP endpoint type, because the module doesn't have this
    // dependency
    ImmutableEndpoint endpoint = getTestOutboundEndpoint("AuthTest", "test://john.doe@xyz.fr");
    MuleEvent event = getTestEvent(new Object());
    Credentials credentials = event.getCredentials();
    assertNull("Credentials must not be created for endpoints without a password.", credentials);
  }

  @Test
  public void testEventSerialization() throws Exception {
    InboundEndpoint endpoint = getTestInboundEndpoint("Test", null, null,
                                                      new PayloadTypeFilter(Object.class), null, null);

    MuleEvent event = getTestEvent("payload", endpoint);
    setCurrentEvent(event);

    Transformer transformer = createSerializableToByteArrayTransformer();
    transformer.setMuleContext(muleContext);
    Serializable serialized = (Serializable) createSerializableToByteArrayTransformer().transform(event);
    assertNotNull(serialized);
    ByteArrayToObject trans = new ByteArrayToObject();
    trans.setMuleContext(muleContext);
    MuleEvent deserialized = (MuleEvent) trans.transform(serialized);

    // Assert that deserialized event is not null and has muleContext
    assertNotNull(deserialized);
    assertNotNull(deserialized.getMuleContext());

    // Assert that deserialized event has session with same id
    assertNotNull(deserialized.getSession());

    // Assert that deserialized event has service and that the service is the same instance
    assertNotNull(deserialized.getFlowConstruct());
    assertEquals(event.getFlowConstruct(), deserialized.getFlowConstruct());
    assertSame(event.getFlowConstruct(), deserialized.getFlowConstruct());

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

    // Assert that deserialized event is not null and has muleContext
    assertNotNull(deserialized);
    assertNotNull(deserialized.getMuleContext());

    // Assert that deserialized event has session with same id
    assertNotNull(deserialized.getSession());

    // Assert that deserialized event has service and that the service is the
    // same instance
    assertNotNull(deserialized.getFlowConstruct());

    Flow flow = (Flow) event.getFlowConstruct();
    Flow deserializedService = (Flow) deserialized.getFlowConstruct();

    // Unable to test services for equality because of need for equals() everywhere. See MULE-3720
    // assertEquals(event.getSession().getService(), deserialized.getSession().getService());
    assertEquals(flow.getName(), deserializedService.getName());
    assertEquals(flow.getInitialState(), deserializedService.getInitialState());
    assertEquals(flow.getExceptionListener().getClass(), deserializedService.getExceptionListener().getClass());
    assertEquals(flow.getMessageProcessors(), deserializedService.getMessageProcessors());

  }

  @Test
  public void testMuleCredentialsSerialization() throws Exception {
    String username = "mule";
    String password = "rulez";
    String url = "test://" + username + ":" + password + "@localhost";
    InboundEndpoint endpoint = getTestInboundEndpoint("Test", url);
    ByteArrayToObject trans = new ByteArrayToObject();
    trans.setMuleContext(muleContext);

    MuleEvent event = getTestEvent("payload", endpoint);
    setCurrentEvent(event);
    Serializable serialized = (Serializable) createSerializableToByteArrayTransformer().transform(event);
    assertNotNull(serialized);

    MuleEvent deserialized = (MuleEvent) trans.transform(serialized);
    assertNotNull(deserialized);

    Credentials credentials = deserialized.getCredentials();
    assertNotNull(credentials);
    assertEquals(username, credentials.getUsername());
    assertTrue(Arrays.equals(password.toCharArray(), credentials.getPassword()));
  }

  private MuleEvent createEventToSerialize() throws Exception {
    createAndRegisterTransformersEndpointBuilderService();
    InboundEndpoint endpoint = getEndpointFactory().getInboundEndpoint(
                                                                       MuleEndpointTestUtils
                                                                           .lookupEndpointBuilder(muleContext.getRegistry(),
                                                                                                  "epBuilderTest"));
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

    List<Transformer> transformers = new ArrayList<Transformer>();
    transformers.add(trans1);
    transformers.add(trans2);

    Filter filter = new PayloadTypeFilter(Object.class);
    EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder("test://serializationTest",
                                                                     muleContext);
    endpointBuilder.setTransformers(transformers);
    endpointBuilder.setName("epBuilderTest");
    endpointBuilder.addMessageProcessor(new MessageFilter(filter));
    registerEndpointBuilder(muleContext.getRegistry(), "epBuilderTest", endpointBuilder);

    getTestFlow();
  }

  private static class TestEventTransformer extends AbstractTransformer {

    @Override
    public Object doTransform(Object src, Charset encoding) throws TransformerException {
      return "Transformed Test Data";
    }
  }

  public void registerEndpointBuilder(MuleRegistry registry, String name, EndpointBuilder builder) throws MuleException {
    registry.registerObject(name, builder, EndpointBuilder.class);
  }
}
