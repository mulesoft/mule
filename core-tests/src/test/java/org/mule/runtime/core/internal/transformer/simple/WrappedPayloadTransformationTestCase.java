/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.junit.Assert.assertEquals;
import static org.mule.runtime.api.message.Message.of;
import org.mule.runtime.core.api.transformer.TransformerException;

import org.junit.Test;

public class WrappedPayloadTransformationTestCase extends HexStringByteArrayTransformersTestCase {

  // extra test for MULE-1274: transforming Mule Messages with regular payload
  @Test
  public void testPayloadWrappedInMuleMessage() throws TransformerException {
    Object wrappedPayload = of(this.getResultData());
    assertEquals(this.getTestData(), this.getRoundTripTransformer().transform(wrappedPayload));
  }

}
