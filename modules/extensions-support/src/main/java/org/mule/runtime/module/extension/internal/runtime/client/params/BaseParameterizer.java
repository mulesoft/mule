/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.params;

import static org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate.RETRY_COUNT_FOREVER;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.api.util.DataSize;
import org.mule.runtime.api.util.Pair;
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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Base implementation of {@link ComponentParameterizer}
 *
 * @since 4.6.0
 */
public abstract class BaseParameterizer<T extends ComponentParameterizer> implements ComponentParameterizer<T> {

  private static final CursorProviderFactory NULL_CURSOR_PROVIDER_FACTORY = new NullCursorProviderFactory();

  private String configRef;
  private final Map<String, Object> rawParameters = new HashMap<>();
  private final Map<Pair<String, String>, Object> groupedParameters = new HashMap<>();
  private Function<StreamingManager, CursorProviderFactory> cursorProviderFunction = sm -> NULL_CURSOR_PROVIDER_FACTORY;
  private RetryPolicyTemplate retryPolicyTemplate = new NoRetryPolicyTemplate();

  @Override
  public T withConfigRef(String configRef) {
    this.configRef = configRef;
    return (T) this;
  }

  @Override
  public T withParameter(String parameterName, Object value) {
    rawParameters.put(parameterName, value);
    return (T) this;
  }

  @Override
  public T withParameter(String parameterGroup, String parameter, Object value) {
    groupedParameters.put(new Pair<>(parameterGroup, parameter), value);
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
                                                                                                                 maxBufferSize));
    return (T) this;
  }

  @Override
  public T withFileStoreRepeatableStreaming(DataSize maxInMemorySize) {
    cursorProviderFunction = sm -> sm.forBytes().getFileStoreCursorStreamProviderFactory(
                                                                                         new FileStoreCursorStreamConfig(maxInMemorySize));
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

  public <T extends ComponentModel> void setValuesOn(ComponentParameterization.Builder<T> builder) {
    rawParameters.forEach(builder::withParameter);
    groupedParameters.forEach((pair, value) -> builder.withParameter(pair.getFirst(), pair.getSecond(), value));
  }
}
