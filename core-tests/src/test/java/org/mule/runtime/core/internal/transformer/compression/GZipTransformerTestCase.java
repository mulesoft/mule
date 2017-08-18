/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.compression;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.util.compression.GZipCompression;
import org.mule.tck.core.transformer.AbstractTransformerTestCase;

import org.junit.Test;

/**
 * Tests {@link GZipCompressTransformer} and its counterpart, the {@link GZipUncompressTransformer}.
 */
public class GZipTransformerTestCase extends AbstractTransformerTestCase {

  protected static final String TEST_DATA =
      "the quick brown fox jumped over the lazy dog the quick brown fox jumped over the lazy dog the quick brown fox jumped over the lazy dog";
  protected GZipCompression strat;

  @Override
  protected void doSetUp() throws Exception {
    strat = new GZipCompression();
  }

  @Override
  public Object getResultData() {
    try {
      return strat.compressByteArray(TEST_DATA.getBytes());
    } catch (Exception e) {
      fail(e.getMessage());
      return null;
    }
  }

  @Override
  public Object getTestData() {
    return TEST_DATA;
  }

  @Override
  public Transformer getTransformer() {
    GZipCompressTransformer transformer = new GZipCompressTransformer();
    transformer.setMuleContext(muleContext);

    return transformer;
  }

  @Override
  public Transformer getRoundTripTransformer() {
    GZipUncompressTransformer transformer = new GZipUncompressTransformer();
    transformer.setMuleContext(muleContext);
    transformer.setReturnDataType(DataType.STRING);

    try {
      transformer.initialise();
    } catch (InitialisationException e) {
      fail(e.getMessage());
    }

    return transformer;
  }

  @Test
  public void testCompressAndDecompress() throws Exception {
    Transformer compressorTransformer = getTransformer();
    Transformer decompressorTransformer = getRoundTripTransformer();

    // Compress the test data.
    Object compressedData = compressorTransformer.transform(getTestData());
    // Decompress the test data.
    Object decompressedData = decompressorTransformer.transform(compressedData);

    assertTrue(String.format("Compress and decompress process failed. Expected '%s', but got '%s'", getTestData(),
                             decompressedData),
               compareResults(getTestData(), decompressedData));
  }
}
