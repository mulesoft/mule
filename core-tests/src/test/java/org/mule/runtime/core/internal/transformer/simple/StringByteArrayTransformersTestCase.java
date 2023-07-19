/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.transformer.simple;

import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.transformer.simple.ObjectToByteArray;
import org.mule.runtime.core.privileged.transformer.simple.ByteArrayToObject;
import org.mule.tck.core.transformer.AbstractTransformerTestCase;

import java.util.Arrays;

public class StringByteArrayTransformersTestCase extends AbstractTransformerTestCase {

  public Transformer getTransformer() throws Exception {
    return new ObjectToByteArray();
  }

  public Transformer getRoundTripTransformer() throws Exception {
    return new ByteArrayToObject();
  }

  public Object getTestData() {
    return "Test";
  }

  public Object getResultData() {
    return "Test".getBytes();
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
}
