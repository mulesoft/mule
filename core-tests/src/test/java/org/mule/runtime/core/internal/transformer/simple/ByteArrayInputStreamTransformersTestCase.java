/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
