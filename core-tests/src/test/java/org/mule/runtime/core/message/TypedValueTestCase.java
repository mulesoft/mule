/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import static java.nio.charset.StandardCharsets.UTF_16;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.builder;
import static org.mule.runtime.api.metadata.TypedValue.of;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.tck.testmodels.fruit.Apple;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import org.junit.Test;

public class TypedValueTestCase {

  private static String STRING_VALUE = "MY_VALUE";
  private static byte[] BYTE_ARRAY_VALUE = "MY_VALUE".getBytes();
  private static InputStream INPUT_STREAM_VALUE = new ByteArrayInputStream(BYTE_ARRAY_VALUE);

  @Test
  public void string() {
    TypedValue<String> typedValue = of(STRING_VALUE);
    assertThat(typedValue.getLength().isPresent(), is(true));
    assertThat(typedValue.getLength().get(), is((long) STRING_VALUE.length()));
  }

  @Test
  public void stringUTF16() {
    TypedValue<String> typedValue = new TypedValue<>(STRING_VALUE, builder(DataType.STRING).charset(UTF_16).build());
    assertThat(typedValue.getLength().isPresent(), is(true));
    assertThat(typedValue.getLength().get(), is((long) STRING_VALUE.getBytes(UTF_16).length));
  }

  @Test
  public void byteArray() {
    TypedValue<byte[]> typedValue = of(BYTE_ARRAY_VALUE);
    assertThat(typedValue.getLength().isPresent(), is(true));
    assertThat(typedValue.getLength().get(), is((long) BYTE_ARRAY_VALUE.length));
  }

  @Test
  public void inputStream() {
    assertThat(of(INPUT_STREAM_VALUE).getLength().isPresent(), is(false));
  }

  @Test
  public void inputStreamWithLength() {
    TypedValue<InputStream> typedValue =
        new TypedValue<>(INPUT_STREAM_VALUE, OBJECT, Optional.of((long) BYTE_ARRAY_VALUE.length));
    assertThat(typedValue.getLength().isPresent(), is(true));
    assertThat(typedValue.getLength().get(), is((long) BYTE_ARRAY_VALUE.length));
  }

  @Test
  public void object() {
    assertThat(of(new Apple()).getLength().isPresent(), is(false));
  }

}
