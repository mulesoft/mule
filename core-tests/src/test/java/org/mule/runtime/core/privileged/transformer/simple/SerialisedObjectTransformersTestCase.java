/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.transformer.simple;

import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.tck.core.transformer.AbstractTransformerTestCase;
import org.mule.tck.testmodels.fruit.Orange;

import org.apache.commons.lang3.SerializationUtils;

public class SerialisedObjectTransformersTestCase extends AbstractTransformerTestCase {

  private Orange testObject = new Orange(new Integer(4), new Double(14.3), "nice!");

  @Override
  public Transformer getTransformer() throws Exception {
    SerializableToByteArray transfromer = new SerializableToByteArray();
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject(String.valueOf(transfromer.hashCode()), transfromer);
    return transfromer;
  }

  @Override
  public Transformer getRoundTripTransformer() throws Exception {
    ByteArrayToSerializable transfromer = new ByteArrayToSerializable();
    ((MuleContextWithRegistries) muleContext).getRegistry().registerObject(String.valueOf(transfromer.hashCode()), transfromer);
    return transfromer;
  }

  @Override
  public Object getTestData() {
    return testObject;
  }

  @Override
  public Object getResultData() {
    return SerializationUtils.serialize(testObject);
  }

}
