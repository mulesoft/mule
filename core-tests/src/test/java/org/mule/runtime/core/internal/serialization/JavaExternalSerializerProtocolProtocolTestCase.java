/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.serialization;

import org.mule.runtime.api.serialization.SerializationException;
import org.mule.tck.core.internal.serialization.AbstractSerializerProtocolContractTestCase;

import org.junit.Test;

public class JavaExternalSerializerProtocolProtocolTestCase extends AbstractSerializerProtocolContractTestCase {

  @Override
  protected void doSetUp() throws Exception {
    serializationProtocol = muleContext.getObjectSerializer().getExternalProtocol();
  }

  @Test(expected = SerializationException.class)
  public void notSerializable() throws Exception {
    serializationProtocol.serialize(new Object());
  }

}
