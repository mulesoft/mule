/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.privileged.transformer.simple.SerialisedObjectTransformersTestCase;

public class ObjectToByteArrayTransformerWithByteTestCase extends SerialisedObjectTransformersTestCase {

  private byte testObject = 'a';

  @Override
  public Transformer getTransformer() throws Exception {
    ObjectToByteArray transfromer = new ObjectToByteArray();
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(String.valueOf(transfromer.hashCode()), transfromer);
    return transfromer;
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    return null;
  }

  @Override
  public Object getTestData() {
    return testObject;
  }

  @Override
  public Object getResultData() {
    return new byte[] {testObject};
  }

}
