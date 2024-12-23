/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.junit.Assert.assertEquals;
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
    transformer.setObjectSerializer(objectSerializer);
  }

  @Test
  public void testTransformString() throws TransformerException, IOException {
    assertTrue(InputStream.class.isAssignableFrom(transformer.transform(TEST_MESSAGE).getClass()));
    assertTrue(compare(new ByteArrayInputStream(TEST_MESSAGE.getBytes()), (InputStream) transformer.transform(TEST_MESSAGE)));
  }

  @Test
  public void testTransformByteArray() throws TransformerException, IOException {
    assertTrue(InputStream.class.isAssignableFrom(transformer.transform(TEST_MESSAGE.getBytes()).getClass()));
    assertTrue(compare(new ByteArrayInputStream(TEST_MESSAGE.getBytes()), (InputStream) transformer.transform(TEST_MESSAGE)));
  }

  @Test
  public void testTransformInputStream() {
    InputStream inputStream = new ByteArrayInputStream(TEST_MESSAGE.getBytes());
    try {
      assertEquals(inputStream, transformer.transform(inputStream));
    } catch (Exception e) {
      assertTrue(e instanceof TransformerException);
      assertTrue(e.getMessage().contains("does not support source type"));
    }
  }

  @Test
  public void testTransformSerializable() {
    Apple apple = new Apple();
    InputStream serializedApple =
        new ByteArrayInputStream(objectSerializer.getExternalProtocol().serialize(apple));
    try {
      assertTrue(compare(serializedApple, (InputStream) transformer.transform(apple)));
    } catch (Exception e) {
      assertTrue(e instanceof TransformerException);
      assertTrue(e.getMessage().contains("does not support source type"));
    }
  }

  public static boolean compare(InputStream input1, InputStream input2) {
    byte[] bytes1 = IOUtils.toByteArray(input1);
    byte[] bytes2 = IOUtils.toByteArray(input2);
    return Arrays.equals(bytes1, bytes2);
  }

}
