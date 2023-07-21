/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.transformer.simple;

import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.privileged.transformer.simple.ByteArrayToObject;
import org.mule.runtime.core.privileged.transformer.simple.SerialisedObjectTransformersTestCase;

public class ObjectByteArrayTransformersWithObjectsTestCase extends SerialisedObjectTransformersTestCase {

  @Override
  public Transformer getTransformer() throws Exception {
    ObjectToByteArray transfromer = new ObjectToByteArray();
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(String.valueOf(transfromer.hashCode()), transfromer);
    return transfromer;
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    ByteArrayToObject transfromer = new ByteArrayToObject();
    ((MuleContextWithRegistry) muleContext).getRegistry().registerObject(String.valueOf(transfromer.hashCode()), transfromer);
    return transfromer;
  }

}
