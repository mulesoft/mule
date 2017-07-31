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

import java.io.UnsupportedEncodingException;

/**
 * Tests {@link GZipCompressTransformer} and its counterpart, the {@link GZipUncompressTransformer} with raw bytes as input.
 */
public class GZipTransformerRawBytesTestCase extends GZipTransformerTestCase {

  @Override
  public Object getResultData() {
    try {
      return strat.compressByteArray((byte[]) this.getTestData());
    } catch (Exception e) {
      fail(e.getMessage());
      return null;
    }
  }

  @Override
  public Object getTestData() {
    try {
      return ((String) super.getTestData()).getBytes("UTF8");
    } catch (UnsupportedEncodingException uex) {
      fail(uex.getMessage());
      return null;
    }
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

  @Override
  public void doTestBadReturnType(Transformer tran, Object src) throws Exception {
    /*
     * Disabled, otherwise the test for invalid return types would fail. The "invalid" class that is configured as returnType by
     * the test harness would actually be a valid return type for our roundTripTransformer.
     */
  }

}
