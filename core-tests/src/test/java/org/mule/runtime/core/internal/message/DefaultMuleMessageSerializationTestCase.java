/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import static org.mule.runtime.core.internal.context.DefaultMuleContext.currentMuleContext;
import static org.mule.test.allure.AllureConstants.SerializationFeature.SERIALIZATION;
import static org.mule.test.allure.AllureConstants.SerializationFeature.SerializationStory.MESSAGE_SERIALIZATION;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.transformer.simple.ObjectToByteArray;
import org.mule.runtime.core.privileged.transformer.TransformersRegistry;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.lang3.SerializationUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SERIALIZATION)
@Story(MESSAGE_SERIALIZATION)
public class DefaultMuleMessageSerializationTestCase extends AbstractMuleContextTestCase {

  private static final String INNER_TEST_MESSAGE = "TestTestTestHello";

  @Before
  public void setUp() {
    currentMuleContext.set(muleContext);
  }

  @After
  public void tearDown() {
    currentMuleContext.set(null);
  }

  @Test
  public void testSerializablePayload() throws Exception {
    final Message message = InternalMessage.builder().value(TEST_MESSAGE).addOutboundProperty("foo", "bar").build();
    Message deserializedMessage = serializationRoundtrip(message);

    assertEquals(TEST_MESSAGE, deserializedMessage.getPayload().getValue());
    assertEquals("bar", ((InternalMessage) deserializedMessage).getOutboundProperty("foo"));
  }

  @Test
  public void testNonSerializablePayload() throws Exception {
    // add a transformer to the registry that can convert a NonSerializable to byte[]. This
    // will be used during Serialization
    ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(TransformersRegistry.class)
        .registerTransformer(new NonSerializableToByteArray());

    final Message message = InternalMessage.builder().value(new NonSerializable()).addOutboundProperty("foo", "bar").build();

    InternalMessage deserializedMessage = serializationRoundtrip(message);

    assertTrue(deserializedMessage.getPayload().getValue() instanceof byte[]);
    assertEquals(INNER_TEST_MESSAGE, getPayloadAsString(deserializedMessage));
  }

  @Test
  public void testStreamPayloadSerialization() throws Exception {
    InputStream stream = new ByteArrayInputStream(TEST_MESSAGE.getBytes());
    final Message message = InternalMessage.builder().value(stream).addOutboundProperty("foo", "bar").build();

    InternalMessage deserializedMessage = serializationRoundtrip(message);
    assertEquals(byte[].class, deserializedMessage.getPayload().getDataType().getType());
    byte[] payload = (byte[]) deserializedMessage.getPayload().getValue();
    assertTrue(Arrays.equals(TEST_MESSAGE.getBytes(), payload));
  }

  @Test
  public void messageSerializationKeepsMediaType() throws Exception {
    final Message message = InternalMessage.builder()
        .payload(new TypedValue<>(new ByteArrayInputStream("{\"id\":\"1\"}".getBytes()), DataType.JSON_STRING)).build();
    InternalMessage deserializedMessage = serializationRoundtrip(message);
    assertThat(deserializedMessage.getPayload().getDataType().getMediaType(),
               is(equalTo(message.getPayload().getDataType().getMediaType())));
  }

  private InternalMessage serializationRoundtrip(Message message) throws Exception {
    return (InternalMessage) SerializationUtils.deserialize(SerializationUtils.serialize(message));
  }

  static class NonSerializable {

    private final String content = INNER_TEST_MESSAGE;

    String getContent() {
      return content;
    }
  }

  static class NonSerializableToByteArray extends ObjectToByteArray {

    public NonSerializableToByteArray() {
      super();
      registerSourceType(DataType.fromType(NonSerializable.class));
      setReturnDataType(DataType.BYTE_ARRAY);
    }

    @Override
    public Object doTransform(Object src, Charset outputEncoding) throws TransformerException {
      String content = ((NonSerializable) src).getContent();
      return content.getBytes();
    }
  }
}
