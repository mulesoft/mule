/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.transformer.simple;

import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.transformer.simple.ObjectToByteArray;
import org.mule.runtime.core.internal.transformer.simple.ObjectToInputStream;
import org.mule.tck.core.transformer.AbstractTransformerTestCase;

import java.io.ByteArrayInputStream;

public class ByteArrayInputStreamTransformersTestCase extends AbstractTransformerTestCase {

  public Transformer getTransformer() throws Exception {
    return new ObjectToInputStream();
  }

  public Transformer getRoundTripTransformer() throws Exception {
    return new ObjectToByteArray();
  }

  public Object getTestData() {
    return TEST_MESSAGE.getBytes();
  }

  public Object getResultData() {
    return new ByteArrayInputStream(TEST_MESSAGE.getBytes());
  }

}
