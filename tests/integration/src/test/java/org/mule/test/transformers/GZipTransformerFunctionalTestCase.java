/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.compression.GZipCompression;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

/**
 * Functional test case for the GZipCompressTransformer and GZipUncompressTransformer.
 */
public class GZipTransformerFunctionalTestCase extends AbstractIntegrationTestCase {

  private static final String TEST_DATA =
      "the quick brown fox jumped over the lazy dog the quick brown fox jumped over the lazy dog the quick brown fox jumped over the lazy dog";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/transformers/gzip-transformer-functional-test.xml";
  }

  @Test
  public void testCompressDecompressByteArray() throws Exception {
    byte[] testDataByteArray = TEST_DATA.getBytes();

    // Compress input.
    Event muleEvent = flowRunner("compressInput").withPayload(testDataByteArray).run();
    Message compressedResponse = muleEvent.getMessage();
    assertNotNull(compressedResponse);
    assertTrue(compressedResponse.getPayload().getValue() instanceof byte[]);

    // Decompress response.
    muleEvent = flowRunner("decompressInput").withPayload(compressedResponse.getPayload().getValue()).run();
    Message uncompressedResponse = muleEvent.getMessage();
    assertNotNull(uncompressedResponse);
    assertTrue(uncompressedResponse.getPayload().getValue() instanceof byte[]);

    String uncompressedStr = new String((byte[]) uncompressedResponse.getPayload().getValue());
    assertEquals(TEST_DATA, uncompressedStr);
  }

  @Test
  public void testCompressDecompressInputStream() throws Exception {
    ByteArrayInputStream bis = new ByteArrayInputStream(TEST_DATA.getBytes());

    // Compress input.
    Event muleEvent = flowRunner("compressInput").withPayload(bis).run();
    Message compressedResponse = muleEvent.getMessage();
    assertNotNull(compressedResponse);
    assertTrue(compressedResponse.getPayload().getValue() instanceof InputStream);

    // Decompress response.
    muleEvent = flowRunner("decompressInput").withPayload(compressedResponse.getPayload().getValue()).run();
    Message uncompressedResponse = muleEvent.getMessage();
    assertNotNull(uncompressedResponse);
    assertTrue(uncompressedResponse.getPayload().getValue() instanceof InputStream);

    byte[] uncompressedByteArray = IOUtils.toByteArray((InputStream) uncompressedResponse.getPayload().getValue());
    String uncompressedStr = new String(uncompressedByteArray);
    assertEquals(TEST_DATA, uncompressedStr);
  }

  @Test
  public void testCompressDecompressString() throws Exception {
    // Compress input.
    Event muleEvent = flowRunner("compressInput").withPayload(TEST_DATA).run();
    Message compressedResponse = muleEvent.getMessage();
    assertNotNull(compressedResponse);
    assertTrue(compressedResponse.getPayload().getValue() instanceof byte[]);
    byte[] bytes = new GZipCompression().uncompressByteArray((byte[]) compressedResponse.getPayload().getValue());
    String clientUncompressed = new String(bytes, "UTF8");
    assertEquals(TEST_DATA, clientUncompressed);

    // Decompress response.
    muleEvent = flowRunner("decompressInputString").withPayload(compressedResponse.getPayload().getValue()).run();
    Message uncompressedResponse = muleEvent.getMessage();
    assertNotNull(uncompressedResponse);
    assertTrue(uncompressedResponse.getPayload().getValue() instanceof String);
    assertEquals(TEST_DATA, uncompressedResponse.getPayload().getValue());
  }

}
