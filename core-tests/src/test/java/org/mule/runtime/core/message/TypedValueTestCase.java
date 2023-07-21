/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.message;

import static java.nio.charset.StandardCharsets.UTF_16;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.DataType.builder;
import static org.mule.runtime.api.metadata.TypedValue.of;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.tck.testmodels.fruit.Apple;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.OptionalLong;

import org.junit.Test;

public class TypedValueTestCase {

  private static String STRING_VALUE = "MY_VALUE";
  private static byte[] BYTE_ARRAY_VALUE = "MY_VALUE".getBytes();
  private static InputStream INPUT_STREAM_VALUE = new ByteArrayInputStream(BYTE_ARRAY_VALUE);
  private static Object OBJECT_VALUE = new Integer(0);

  @Test
  public void string() {
    TypedValue<String> typedValue = of(STRING_VALUE);
    assertThat(typedValue.getByteLength().isPresent(), is(true));
    assertThat(typedValue.getByteLength().getAsLong(), is((long) STRING_VALUE.length()));
  }

  @Test
  public void stringUTF16() {
    TypedValue<String> typedValue = new TypedValue<>(STRING_VALUE, builder(STRING).charset(UTF_16).build());
    assertThat(typedValue.getByteLength().isPresent(), is(true));
    assertThat(typedValue.getByteLength().getAsLong(), is((long) STRING_VALUE.getBytes(UTF_16).length));
  }

  @Test
  public void byteArray() {
    TypedValue<byte[]> typedValue = of(BYTE_ARRAY_VALUE);
    assertThat(typedValue.getByteLength().isPresent(), is(true));
    assertThat(typedValue.getByteLength().getAsLong(), is((long) BYTE_ARRAY_VALUE.length));
  }

  @Test
  public void inputStream() {
    assertThat(of(INPUT_STREAM_VALUE).getByteLength().isPresent(), is(false));
  }

  @Test
  public void inputStreamWithLength() {
    TypedValue<InputStream> typedValue =
        new TypedValue<>(INPUT_STREAM_VALUE, OBJECT, OptionalLong.of(BYTE_ARRAY_VALUE.length));
    assertThat(typedValue.getByteLength().isPresent(), is(true));
    assertThat(typedValue.getByteLength().getAsLong(), is((long) BYTE_ARRAY_VALUE.length));
  }

  @Test
  public void object() {
    assertThat(of(new Apple()).getByteLength().isPresent(), is(false));
  }

  @Test
  public void serializeWithoutLength() throws Exception {
    final TypedValue<Object> typedValue = new TypedValue<>(OBJECT_VALUE, DataType.fromObject(OBJECT_VALUE), OptionalLong.empty());
    final TypedValue<Object> deserealized = serializationRoundTrip(typedValue);
    assertThat(deserealized, equalTo(typedValue));
  }

  @Test
  public void serializeWithLength() throws Exception {
    final TypedValue<String> typedValue = new TypedValue<>(STRING_VALUE, STRING);
    final TypedValue<String> deserealized = serializationRoundTrip(typedValue);
    assertThat(deserealized, equalTo(typedValue));
    assertThat(deserealized.getByteLength().getAsLong(), is(new Long(STRING_VALUE.length())));
  }

  private <T> TypedValue<T> serializationRoundTrip(TypedValue<T> typedValue) throws Exception {
    return deserealize(serialize(typedValue));
  }

  private byte[] serialize(Object object) throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try (ObjectOutputStream out = new ObjectOutputStream(outputStream)) {
      out.writeObject(object);
      return outputStream.toByteArray();
    }
  }

  private <T> T deserealize(byte[] bytes) throws Exception {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
    try (ObjectInputStream in = new ObjectInputStream(inputStream)) {
      return (T) in.readObject();
    }
  }
}
