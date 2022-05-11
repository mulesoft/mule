/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.privileged.transformer.CompositeConverter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import io.qameta.allure.Issue;
import org.junit.Test;

@Issue("MULE-19279")
public class ByteArrayToInputStreamCompositeTransformersTestCase extends AbstractMuleContextTestCase {

  private CompositeConverter transformer;
  private String testMessage = "Hello ե���:��";

  @Override
  protected void doSetUp() throws Exception {
    Converter objectToOutputHandler = new ObjectToOutputHandler();
    Converter objectToInputStream = new ObjectToInputStream();
    transformer = new CompositeConverter(objectToOutputHandler, objectToInputStream);
    transformer.setMuleContext(muleContext);
  }

  @Test
  public void testTransformByteArrayUTF16() throws TransformerException, IOException {
    assertTrue(InputStream.class.isAssignableFrom(transformer.transform(testMessage.getBytes("UTF-16")).getClass()));
    assertTrue(compare(new ByteArrayInputStream(testMessage.getBytes("UTF-16")),
                       (InputStream) transformer.transform(testMessage.getBytes("UTF-16"))));
  }

  public static boolean compare(InputStream input1, InputStream input2) {
    byte[] bytes1 = IOUtils.toByteArray(input1);
    byte[] bytes2 = IOUtils.toByteArray(input2);
    return Arrays.equals(bytes1, bytes2);
  }
}
