/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.adapter;

import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.util.DataSize;
import org.mule.sdk.api.client.OperationParameterizer;

class SdkOperationParameterizerAdapter implements OperationParameterizer {

  private final org.mule.runtime.extension.api.client.OperationParameterizer delegate;

  SdkOperationParameterizerAdapter(org.mule.runtime.extension.api.client.OperationParameterizer delegate) {
    this.delegate = delegate;
  }

  @Override
  public OperationParameterizer withConfigRef(String configurationName) {
    delegate.withConfigRef(configurationName);
    return this;
  }

  @Override
  public OperationParameterizer withParameter(String parameterName, Object value) {
    delegate.withParameter(parameterName, value);
    return this;
  }

  @Override
  public OperationParameterizer withParameter(String parameterGroup, String parameterName, Object value) {
    delegate.withParameter(parameterGroup, parameterName, value);
    return this;
  }

  @Override
  public OperationParameterizer withSimpleReconnection(int frequency, int maxAttempts) {
    delegate.withSimpleReconnection(frequency, maxAttempts);
    return this;
  }

  @Override
  public OperationParameterizer reconnectingForever(int frequency) {
    delegate.reconnectingForever(frequency);
    return this;
  }

  @Override
  public OperationParameterizer withDefaultRepeatableStreaming() {
    delegate.withDefaultRepeatableStreaming();
    return this;
  }

  @Override
  public OperationParameterizer withInMemoryRepeatableStreaming(DataSize initialBufferSize,
                                                                DataSize bufferSizeIncrement,
                                                                DataSize maxBufferSize) {
    delegate.withInMemoryRepeatableStreaming(initialBufferSize, bufferSizeIncrement, maxBufferSize);
    return this;
  }

  @Override
  public OperationParameterizer withFileStoreRepeatableStreaming(DataSize maxInMemorySize) {
    delegate.withFileStoreRepeatableStreaming(maxInMemorySize);
    return this;
  }

  @Override
  public OperationParameterizer withDefaultRepeatableIterables() {
    delegate.withDefaultRepeatableIterables();
    return this;
  }

  @Override
  public OperationParameterizer withInMemoryRepeatableIterables(int initialBufferSize,
                                                                int bufferSizeIncrement,
                                                                int maxBufferSize) {
    delegate.withInMemoryRepeatableIterables(initialBufferSize, bufferSizeIncrement, maxBufferSize);
    return this;
  }

  @Override
  public OperationParameterizer withFileStoreRepeatableIterables(int maxInMemoryInstances) {
    delegate.withFileStoreRepeatableIterables(maxInMemoryInstances);
    return this;
  }

  @Override
  public OperationParameterizer inTheContextOf(Event event) {
    delegate.inTheContextOf(event);
    return this;
  }
}
