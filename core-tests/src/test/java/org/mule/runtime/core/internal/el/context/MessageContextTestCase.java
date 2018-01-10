/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.context;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.internal.message.InternalMessage;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

public class MessageContextTestCase extends AbstractELTestCase {

  private CoreEvent event;
  private Message message;

  public MessageContextTestCase(String mvelOptimizer) {
    super(mvelOptimizer);
  }

  @Before
  public void setup() throws MuleException {
    message = spy(Message.of(null));
    event = spy(getEventBuilder().message(message).build());
  }

  @Test
  public void message() throws Exception {
    CoreEvent event = CoreEvent.builder(context).message(Message.of("foo")).build();
    assertTrue(evaluate("message", event) instanceof MessageContext);
    assertEquals("foo", evaluate("message.payload", event));
  }

  @Test
  public void assignToMessage() throws Exception {
    CoreEvent event = CoreEvent.builder(context).message(Message.of("")).build();
    assertImmutableVariable("message='foo'", event);
  }

  @Test
  public void correlationId() throws Exception {
    when(event.getCorrelationId()).thenReturn("3");
    assertEquals("3", evaluate("message.correlationId", event));
    assertFinalProperty("message.correlationId=2", event);
  }

  @Test
  public void correlationSequence() throws Exception {
    when(event.getGroupCorrelation()).thenReturn(Optional.of(GroupCorrelation.of(4)));
    assertEquals(4, evaluate("message.correlationSequence", event));
    assertFinalProperty("message.correlationSequence=2", event);
  }

  @Test
  public void correlationGroupSize() throws Exception {
    when(event.getGroupCorrelation()).thenReturn(Optional.of(GroupCorrelation.of(0, 2)));
    assertEquals(2, evaluate("message.correlationGroupSize", event));
    assertFinalProperty("message.correlationGroupSize=2", event);
  }

  @Test
  public void dataType() throws Exception {
    when(message.getPayload()).thenReturn(new TypedValue<Object>("", DataType.STRING));
    assertThat(evaluate("message.dataType", event), is(DataType.STRING));
    assertFinalProperty("message.mimType=2", event);
  }

  @Test
  public void payload() throws Exception {
    InternalMessage message = mock(InternalMessage.class);
    when(event.getMessage()).thenReturn(message);
    Object payload = new Object();
    when(message.getPayload()).thenReturn(new TypedValue<>(payload, DataType.OBJECT));
    assertSame(payload, evaluate("message.payload", event));
  }

  @Test
  public void assignPayload() throws Exception {
    message = Message.of("");
    CoreEvent.Builder eventBuilder = CoreEvent.builder(event);
    evaluate("message.payload = 'foo'", event, eventBuilder);
    assertThat(eventBuilder.build().getMessage().getPayload().getValue(), equalTo("foo"));
  }

  @Test
  public void payloadAsType() throws Exception {
    InternalMessage transformedMessage = mock(InternalMessage.class, RETURNS_DEEP_STUBS);
    final TypedValue<Object> expectedPayload = new TypedValue<>(new Object(), OBJECT);
    when(transformedMessage.getPayload()).thenReturn(expectedPayload);
    TransformationService transformationService = mock(TransformationService.class);
    muleContext.setTransformationService(transformationService);
    when(transformationService.transform(any(InternalMessage.class), any(DataType.class))).thenReturn(transformedMessage);
    assertSame(transformedMessage.getPayload().getValue(),
               evaluate("message.payloadAs(org.mule.tck.testmodels.fruit.Banana)", event));
  }

  @Test
  public void payloadAsDataType() throws Exception {
    InternalMessage transformedMessage = mock(InternalMessage.class, RETURNS_DEEP_STUBS);
    TransformationService transformationService = mock(TransformationService.class);
    when(transformedMessage.getPayload()).thenReturn(new TypedValue<Object>(TEST_PAYLOAD, STRING));
    muleContext.setTransformationService(transformationService);
    when(transformationService.transform(event.getMessage(), DataType.STRING)).thenReturn(transformedMessage);
    Object result = evaluate("message.payloadAs(" + DataType.class.getName() + ".STRING)", event);
    assertSame(TEST_PAYLOAD, result);
  }

  @Test
  public void nullPayloadTest() throws Exception {
    when(message.getPayload()).thenReturn(null);
    assertEquals(true, evaluate("message.payload == null", event));
    assertEquals(true, evaluate("payload == null", event));
    assertEquals(true, evaluate("message.payload == empty", event));
  }

  @Test
  public void attributes() throws Exception {
    Object attributes = mock(Object.class);
    when(message.getAttributes()).thenReturn(new TypedValue<>(attributes, DataType.OBJECT));
    assertThat(evaluate("message.attributes", event), is(sameInstance(attributes)));
  }
}
