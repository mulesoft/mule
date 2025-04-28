/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.bean;

import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.serialization.SerializationProtocol;
import org.mule.runtime.config.api.bean.ObjectSerializerDelegate;
import org.mule.runtime.core.api.MuleContext;

import jakarta.inject.Inject;

/**
 * @since 4.10
 */
public class DefaultObjectSerializerDelegate implements ObjectSerializerDelegate {

  private ObjectSerializer delegate;

  @Override
  public SerializationProtocol getInternalProtocol() {
    return delegate.getInternalProtocol();
  }

  @Override
  public SerializationProtocol getExternalProtocol() {
    return delegate.getExternalProtocol();
  }

  @Inject
  public void setMuleContext(MuleContext muleContext) {
    this.setDelegate(muleContext.getObjectSerializer());
  }

  @Override
  public void setDelegate(ObjectSerializer delegate) {
    this.delegate = delegate;
  }

  @Override
  public ObjectSerializer getDelegate() {
    return delegate;
  }
}
