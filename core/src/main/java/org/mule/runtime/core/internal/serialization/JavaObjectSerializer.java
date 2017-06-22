/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.serialization;

import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.serialization.SerializationProtocol;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;

/**
 * Serializes objects using the default Java serialization mechanism provided by writeObject and readObject methods.
 */
public class JavaObjectSerializer implements ObjectSerializer, MuleContextAware {

  private volatile JavaExternalSerializerProtocol javaSerializerProtocol = new JavaExternalSerializerProtocol();

  @Override
  public SerializationProtocol getInternalProtocol() {
    return javaSerializerProtocol;
  }

  @Override
  public SerializationProtocol getExternalProtocol() {
    return javaSerializerProtocol;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    javaSerializerProtocol.setMuleContext(context);
  }
}
