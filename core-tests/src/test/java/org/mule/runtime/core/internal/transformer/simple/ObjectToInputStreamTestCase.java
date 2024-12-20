/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static java.nio.charset.Charset.defaultCharset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.internal.serialization.JavaObjectSerializer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

public class ObjectToInputStreamTestCase extends AbstractMuleTestCase {

  public static final String TEST_MESSAGE = "Test Message";

  private ObjectSerializer objectSerializer;
  private ObjectToInputStream transformer;

  @Before
  public void setUp() throws Exception {
    objectSerializer = new JavaObjectSerializer(this.getClass().getClassLoader());

    transformer = new ObjectToInputStream();
    transformer.setArtifactEncoding(() -> defaultCharset());
    transformer.setObjectSerializer(objectSerializer);
  }

  @Test
  public void testTransformString() throws TransformerException, IOException {
    assertThat(transformer.transform(TEST_MESSAGE), instanceOf(InputStream.class));
    assertTrue(compare(new ByteArrayInputStream(TEST_MESSAGE.getBytes()), (InputStream) transformer.transform(TEST_MESSAGE)));
  }

  @Test
  public void testTransformByteArray() throws TransformerException, IOException {
    assertThat(transformer.transform(TEST_MESSAGE), instanceOf(InputStream.class));
    assertTrue(compare(new ByteArrayInputStream(TEST_MESSAGE.getBytes()), (InputStream) transformer.transform(TEST_MESSAGE)));
  }

  @Test
  public void testTransformInputStream() {
    InputStream inputStream = new ByteArrayInputStream(TEST_MESSAGE.getBytes());

    var thrown = assertThrows(TransformerException.class, () -> transformer.transform(inputStream));
    assertThat(thrown.getMessage(), containsString("does not support source type"));
  }

  @Test
  public void testTransformSerializable() throws TransformerException {
    Apple apple = new Apple();
    InputStream serializedApple =
        new ByteArrayInputStream(objectSerializer.getExternalProtocol().serialize(apple));

    assertTrue(compare(serializedApple, (InputStream) transformer.transform(apple)));
  }

  public static boolean compare(InputStream input1, InputStream input2) {
    byte[] bytes1 = IOUtils.toByteArray(input1);
    byte[] bytes2 = IOUtils.toByteArray(input2);
    return Arrays.equals(bytes1, bytes2);
  }

}
