/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.compression;

import static org.junit.Assert.fail;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.Transformer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Tests {@link GZipCompressTransformer} and its counterpart, the {@link GZipUncompressTransformer} with streams as inputs.
 */
public class GZipTransformerStreamTestCase extends GZipTransformerTestCase {

  @Override
  public Object getResultData() {
    try {
      return strat.compressInputStream((InputStream) getTestData());

    } catch (Exception e) {
      fail(e.getMessage());
      return null;
    }
  }

  @Override
  public Object getTestData() {
    return new ByteArrayInputStream(TEST_DATA.getBytes());
  }

  @Override
  public Transformer getRoundTripTransformer() {
    GZipUncompressTransformer transformer = new GZipUncompressTransformer();
    transformer.setMuleContext(muleContext);

    try {
      transformer.initialise();
    } catch (InitialisationException e) {
      fail(e.getMessage());
    }

    return transformer;
  }
}


