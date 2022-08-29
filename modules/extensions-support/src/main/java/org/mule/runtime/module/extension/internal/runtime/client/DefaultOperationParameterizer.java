/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import static org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate.RETRY_COUNT_FOREVER;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.util.DataSize;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.NullCursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.runtime.core.api.streaming.object.FileStoreCursorIteratorConfig;
import org.mule.runtime.core.api.streaming.object.InMemoryCursorIteratorConfig;
import org.mule.runtime.core.api.streaming.bytes.FileStoreCursorStreamConfig;
import org.mule.runtime.extension.api.client.OperationParameterizer;
import org.mule.runtime.extension.api.component.ComponentParameterization;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

class DefaultOperationParameterizer implements OperationParameterizer {

  private static final CursorProviderFactory NULL_CURSOR_PROVIDER_FACTORY = new NullCursorProviderFactory();
  private String configRef;
  private final Map<String, Object> rawParameters = new HashMap<>();
  private final Map<Pair<String, String>, Object> groupedParameters = new HashMap<>();
  private Function<StreamingManager, CursorProviderFactory> cursorProviderFunction = nullCursorProviderFunction();
  private RetryPolicyTemplate retryPolicyTemplate = new NoRetryPolicyTemplate();

  @Override
  public OperationParameterizer withConfigRef(String configRef) {
    this.configRef = configRef;
    return this;
  }

  @Override
  public OperationParameterizer withParameter(String parameterName, Object value) {
    rawParameters.put(parameterName, value);
    return this;
  }

  @Override
  public OperationParameterizer withParameter(String parameterGroup, String parameter, Object value) {
    groupedParameters.put(new Pair<>(parameterGroup, parameter), value);
    return this;
  }

  @Override
  public OperationParameterizer withSimpleReconnection(int frequency, int count) {
    retryPolicyTemplate = new SimpleRetryPolicyTemplate(frequency, count);
    return this;
  }

  @Override
  public OperationParameterizer reconnectingForever(int frequency) {
    retryPolicyTemplate = new SimpleRetryPolicyTemplate(frequency, RETRY_COUNT_FOREVER);
    return this;
  }

  @Override
  public OperationParameterizer withDefaultRepeatableStreaming() {
    cursorProviderFunction = sm -> sm.forBytes().getDefaultCursorProviderFactory();
    return this;
  }

  @Override
  public OperationParameterizer withInMemoryRepeatableStreaming(DataSize initialBufferSize,
                                                                DataSize bufferSizeIncrement,
                                                                DataSize maxBufferSize) {
    cursorProviderFunction = sm -> sm.forBytes().getInMemoryCursorProviderFactory(
                                                                                  new InMemoryCursorStreamConfig(initialBufferSize,
                                                                                                                 bufferSizeIncrement,
                                                                                                                 maxBufferSize));
    return this;
  }

  @Override
  public OperationParameterizer withFileStoreRepeatableStreaming(DataSize maxInMemorySize) {
    cursorProviderFunction = sm -> sm.forBytes().getFileStoreCursorStreamProviderFactory(
                                                                                         new FileStoreCursorStreamConfig(maxInMemorySize));
    return this;
  }

  @Override
  public OperationParameterizer withDefaultRepeatableIterables() {
    cursorProviderFunction = sm -> sm.forObjects().getDefaultCursorProviderFactory();
    return this;
  }

  @Override
  public OperationParameterizer withInMemoryRepeatableIterables(int initialBufferSize, int bufferSizeIncrement,
                                                                int maxBufferSize) {
    cursorProviderFunction = sm -> sm.forObjects().getInMemoryCursorProviderFactory(
                                                                                    new InMemoryCursorIteratorConfig(initialBufferSize,
                                                                                                                     bufferSizeIncrement,
                                                                                                                     maxBufferSize));
    return this;
  }

  @Override
  public OperationParameterizer withFileStoreRepeatableIterables(int maxInMemoryInstances) {
    cursorProviderFunction = sm -> sm.forObjects().getFileStoreCursorIteratorProviderFactory(
                                                                                             new FileStoreCursorIteratorConfig(maxInMemoryInstances));
    return this;
  }

  @Override
  public OperationParameterizer withNonRepeatableStreaming() {
    cursorProviderFunction = nullCursorProviderFunction();
    return this;
  }

  String getConfigRef() {
    return configRef;
  }

  <T> CursorProviderFactory<T> getCursorProviderFactory(StreamingManager streamingManager) {
    return cursorProviderFunction.apply(streamingManager);
  }

  RetryPolicyTemplate getRetryPolicyTemplate() {
    return retryPolicyTemplate;
  }

  void setValuesOn(ComponentParameterization.Builder<OperationModel> builder) {
    rawParameters.forEach(builder::withParameter);
    groupedParameters.forEach((pair, value) -> builder.withParameter(pair.getFirst(), pair.getSecond(), value));
  }

  private Function<StreamingManager, CursorProviderFactory> nullCursorProviderFunction() {
    return sm -> NULL_CURSOR_PROVIDER_FACTORY;
  }
}
