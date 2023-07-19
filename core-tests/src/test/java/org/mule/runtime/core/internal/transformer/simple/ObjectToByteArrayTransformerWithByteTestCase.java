/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
