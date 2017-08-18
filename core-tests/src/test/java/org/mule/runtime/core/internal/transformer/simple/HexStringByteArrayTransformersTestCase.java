/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.transformer.simple.ByteArrayToHexString;
import org.mule.runtime.core.internal.transformer.simple.HexStringToByteArray;
import org.mule.tck.core.transformer.AbstractTransformerTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HexStringByteArrayTransformersTestCase extends AbstractTransformerTestCase {

  public Transformer getTransformer() {
    return new HexStringToByteArray();
  }

  public Transformer getRoundTripTransformer() {
    return new ByteArrayToHexString();
  }

  public Object getTestData() {
    return "01020aff";
  }

  public Object getResultData() {
    return new byte[] {1, 2, 10, (byte) 0xff};
  }

  @Override
  public boolean compareResults(Object src, Object result) {
    if (src == null && result == null) {
      return true;
    }
    if (src == null || result == null) {
      return false;
    }
    return Arrays.equals((byte[]) src, (byte[]) result);
  }

  @Override
  public boolean compareRoundtripResults(Object src, Object result) {
    if (src == null && result == null) {
      return true;
    }
    if (src == null || result == null) {
      return false;
    }
    return src.equals(result);
  }

  // extra test for uppercase output
  @Test
  public void testUppercase() throws TransformerException {
    ByteArrayToHexString t = new ByteArrayToHexString();
    t.setUpperCase(true);

    assertEquals(((String) getTestData()).toUpperCase(), t.transform(getResultData()));
  }

  @Test
  public void testStreaming() throws TransformerException {
    ByteArrayToHexString transformer = new ByteArrayToHexString();
    InputStream input = new ByteArrayInputStream((byte[]) this.getResultData());

    assertEquals(this.getTestData(), transformer.transform(input));
  }

}
