/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transformer;

import static org.junit.Assert.fail;
import org.mule.runtime.api.metadata.DataType;
import org.mule.tck.core.transformer.AbstractTransformerTestCase;

import java.nio.charset.Charset;

import org.junit.Test;

public class NullResultTestCase extends AbstractTransformerTestCase {

  private final NullResultTransformer transformer = new NullResultTransformer();

  @Override
  public Object getTestData() {
    return new Object();
  }

  @Override
  public Object getResultData() {
    return null;
  }

  @Override
  public Transformer getTransformer() throws Exception {
    return transformer;
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    return null;
  }

  @Test
  public void testNullNotExpected() throws Exception {
    transformer.setReturnDataType(DataType.STRING);
    try {
      testTransform();
      fail("Transformer should have thrown an exception because the return class doesn't match the result.");
    } catch (TransformerException e) {
      // expected
    }
  }

  public static final class NullResultTransformer extends AbstractTransformer {

    public NullResultTransformer() {
      super();
      this.registerSourceType(DataType.OBJECT);
      this.setReturnDataType(DataType.fromObject(null));
    }

    @Override
    public Object doTransform(Object src, Charset encoding) throws TransformerException {
      return null;
    }
  }
}
