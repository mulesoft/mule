/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.params;

import static org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate.RETRY_COUNT_FOREVER;

import org.mule.runtime.api.util.DataSize;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.NullCursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.bytes.FileStoreCursorStreamConfig;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.runtime.core.api.streaming.object.FileStoreCursorIteratorConfig;
import org.mule.runtime.core.api.streaming.object.InMemoryCursorIteratorConfig;
import org.mule.runtime.extension.api.client.params.ComponentParameterizer;

import java.util.function.Function;

/**
 * Base implementation of {@link ComponentParameterizer}
 *
 * @since 4.5.0
 */
public abstract class BaseComponentParameterizer<T extends ComponentParameterizer> extends BaseParameterizer<T>
    implements ComponentParameterizer<T> {

  private static final CursorProviderFactory NULL_CURSOR_PROVIDER_FACTORY = new NullCursorProviderFactory();

  private String configRef;
  private Function<StreamingManager, CursorProviderFactory> cursorProviderFunction = sm -> NULL_CURSOR_PROVIDER_FACTORY;
  private RetryPolicyTemplate retryPolicyTemplate = new NoRetryPolicyTemplate();

  @Override
  public T withConfigRef(String configRef) {
    this.configRef = configRef;
    return (T) this;
  }

  @Override
  public T withSimpleReconnection(int frequency, int maxAttempts) {
    retryPolicyTemplate = new SimpleRetryPolicyTemplate(frequency, maxAttempts);
    return (T) this;
  }

  @Override
  public T reconnectingForever(int frequency) {
    withSimpleReconnection(frequency, RETRY_COUNT_FOREVER);
    return (T) this;
  }

  @Override
  public T withDefaultRepeatableStreaming() {
    cursorProviderFunction = sm -> sm.forBytes().getDefaultCursorProviderFactory();
    return (T) this;
  }

  @Override
  public T withInMemoryRepeatableStreaming(DataSize initialBufferSize,
                                           DataSize bufferSizeIncrement,
                                           DataSize maxBufferSize) {
    cursorProviderFunction = sm -> sm.forBytes().getInMemoryCursorProviderFactory(
                                                                                  new InMemoryCursorStreamConfig(initialBufferSize,
                                                                                                                 bufferSizeIncrement,
                                                                                                                 maxBufferSize,
                                                                                                                 false));
    return (T) this;
  }

  @Override
  public T withFileStoreRepeatableStreaming(DataSize maxInMemorySize) {
    cursorProviderFunction = sm -> sm.forBytes().getFileStoreCursorStreamProviderFactory(
                                                                                         new FileStoreCursorStreamConfig(maxInMemorySize,
                                                                                                                         false));
    return (T) this;
  }

  @Override
  public T withDefaultRepeatableIterables() {
    cursorProviderFunction = sm -> sm.forObjects().getDefaultCursorProviderFactory();
    return (T) this;
  }

  @Override
  public T withInMemoryRepeatableIterables(int initialBufferSize, int bufferSizeIncrement,
                                           int maxBufferSize) {
    cursorProviderFunction = sm -> sm.forObjects().getInMemoryCursorProviderFactory(
                                                                                    new InMemoryCursorIteratorConfig(initialBufferSize,
                                                                                                                     bufferSizeIncrement,
                                                                                                                     maxBufferSize));
    return (T) this;
  }

  @Override
  public T withFileStoreRepeatableIterables(int maxInMemoryInstances) {
    cursorProviderFunction = sm -> sm.forObjects().getFileStoreCursorIteratorProviderFactory(
                                                                                             new FileStoreCursorIteratorConfig(maxInMemoryInstances));
    return (T) this;
  }

  public String getConfigRef() {
    return configRef;
  }

  public <T> CursorProviderFactory<T> getCursorProviderFactory(StreamingManager streamingManager) {
    return cursorProviderFunction.apply(streamingManager);
  }

  public RetryPolicyTemplate getRetryPolicyTemplate() {
    return retryPolicyTemplate;
  }
}
