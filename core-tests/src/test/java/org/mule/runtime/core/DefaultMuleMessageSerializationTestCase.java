/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.transformer.simple.ObjectToByteArray;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Test;

public class DefaultMuleMessageSerializationTestCase extends AbstractMuleContextTestCase {

  private static final String INNER_TEST_MESSAGE = "TestTestTestHello";

  @Test
  public void testSerializablePayload() throws Exception {
    final Message message = InternalMessage.builder().payload(TEST_MESSAGE).addOutboundProperty("foo", "bar").build();
    Message deserializedMessage = serializationRoundtrip(message);

    assertEquals(TEST_MESSAGE, deserializedMessage.getPayload().getValue());
    assertEquals("bar", ((InternalMessage) deserializedMessage).getOutboundProperty("foo"));
  }

  @Test
  public void testNonSerializablePayload() throws Exception {
    // add a transformer to the registry that can convert a NonSerializable to byte[]. This
    // will be used during Serialization
    muleContext.getRegistry().registerTransformer(new NonSerializableToByteArray());

    final Message message = InternalMessage.builder().payload(new NonSerializable()).addOutboundProperty("foo", "bar").build();

    setCurrentEvent(eventBuilder().message(message).build());
    InternalMessage deserializedMessage = serializationRoundtrip(message);

    assertTrue(deserializedMessage.getPayload().getValue() instanceof byte[]);
    assertEquals(INNER_TEST_MESSAGE, getPayloadAsString(deserializedMessage));
  }

  @Test
  public void testStreamPayloadSerialization() throws Exception {
    InputStream stream = new ByteArrayInputStream(TEST_MESSAGE.getBytes());
    final Message message = InternalMessage.builder().payload(stream).addOutboundProperty("foo", "bar").build();
    setCurrentEvent(eventBuilder().message(message).build());
    InternalMessage deserializedMessage = serializationRoundtrip(message);

    assertEquals(byte[].class, deserializedMessage.getPayload().getDataType().getType());
    byte[] payload = (byte[]) deserializedMessage.getPayload().getValue();
    assertTrue(Arrays.equals(TEST_MESSAGE.getBytes(), payload));
  }

  private InternalMessage serializationRoundtrip(Message message) throws Exception {
    return (InternalMessage) SerializationUtils.deserialize(SerializationUtils.serialize(message));
  }

  static class NonSerializable {

    private String content = INNER_TEST_MESSAGE;

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
