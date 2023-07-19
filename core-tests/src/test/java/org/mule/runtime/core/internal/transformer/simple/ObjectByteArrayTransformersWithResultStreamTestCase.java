/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.transformer.simple;

import org.mule.runtime.core.api.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ObjectByteArrayTransformersWithResultStreamTestCase extends ObjectByteArrayTransformersWithObjectsTestCase {

  @Override
  public Object getResultData() {
    byte[] resultData = (byte[]) super.getResultData();
    return new ByteArrayInputStream(resultData);
  }

  @Override
  public boolean compareResults(Object expected, Object result) {
    if (expected instanceof InputStream) {
      InputStream input = (InputStream) expected;
      byte[] bytes = IOUtils.toByteArray(input);
      return super.compareResults(bytes, result);
    }
    return super.compareResults(expected, result);
  }

}
