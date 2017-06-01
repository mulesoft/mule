/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Test;

public class ObjectToInputStreamTestCase extends AbstractMuleContextTestCase {

  private ObjectToInputStream transformer;

  @Override
  protected void doSetUp() throws Exception {
    transformer = new ObjectToInputStream();
    transformer.setMuleContext(muleContext);
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
        new ByteArrayInputStream(muleContext.getObjectSerializer().getExternalProtocol().serialize(apple));
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
