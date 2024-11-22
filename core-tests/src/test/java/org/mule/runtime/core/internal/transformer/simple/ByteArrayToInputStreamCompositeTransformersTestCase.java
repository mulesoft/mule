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
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.internal.transformer.CompositeConverter;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Issue;

@Issue("MULE-19279")
public class ByteArrayToInputStreamCompositeTransformersTestCase extends AbstractMuleTestCase {

  private CompositeConverter transformer;
  private String testMessage = "Hello ե���:��";

  @Before
  public void doSetUp() throws Exception {
    ObjectToOutputHandler objectToOutputHandler = new ObjectToOutputHandler();
    objectToOutputHandler.setArtifactEncoding(() -> defaultCharset());
    ObjectToInputStream objectToInputStream = new ObjectToInputStream();
    objectToInputStream.setArtifactEncoding(() -> defaultCharset());
    transformer = new CompositeConverter(objectToOutputHandler, objectToInputStream);
  }

  @Test
  public void testTransformByteArrayUTF16() throws TransformerException, IOException {
    assertThat(transformer.transform(testMessage.getBytes("UTF-16")), instanceOf(InputStream.class));
    assertTrue(compare(new ByteArrayInputStream(testMessage.getBytes("UTF-16")),
                       (InputStream) transformer.transform(testMessage.getBytes("UTF-16"))));
  }

  public static boolean compare(InputStream input1, InputStream input2) {
    byte[] bytes1 = IOUtils.toByteArray(input1);
    byte[] bytes2 = IOUtils.toByteArray(input2);
    return Arrays.equals(bytes1, bytes2);
  }
}
