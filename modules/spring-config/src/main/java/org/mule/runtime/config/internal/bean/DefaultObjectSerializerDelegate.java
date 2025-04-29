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

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import jakarta.inject.Inject;

public class DefaultObjectSerializerDelegate implements ObjectSerializerDelegate {

  private ReadWriteLock delegateLock = new ReentrantReadWriteLock();

  private volatile ObjectSerializer delegate;

  @Override
  public SerializationProtocol getInternalProtocol() {
    return getDelegate().getInternalProtocol();
  }

  @Override
  public SerializationProtocol getExternalProtocol() {
    return getDelegate().getExternalProtocol();
  }

  @Inject
  public void setMuleContext(MuleContext muleContext) {
    this.setDelegate(muleContext.getObjectSerializer());
  }

  @Override
  public void setDelegate(ObjectSerializer delegate) {
    delegateLock.writeLock().lock();
    try {
      this.delegate = delegate;
    } finally {
      delegateLock.writeLock().unlock();
    }
  }

  @Override
  public ObjectSerializer getDelegate() {
    delegateLock.readLock().lock();
    try {
      return delegate;
    } finally {
      delegateLock.readLock().unlock();
    }
  }
}
