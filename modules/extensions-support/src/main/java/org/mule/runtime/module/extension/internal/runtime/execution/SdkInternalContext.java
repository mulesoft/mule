/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.util.function.Function.identity;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.EventInternalContext;
import org.mule.runtime.core.internal.policy.OperationPolicy;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import reactor.util.context.Context;

public class SdkInternalContext implements EventInternalContext<SdkInternalContext> {

  private OperationExecutionParams operationExecutionParams;

  private Function<Context, Context> innerChainSubscriberContextMapping = identity();

  private Optional<ConfigurationInstance> configuration;

  private Map<String, Object> resolutionResult;

  private OperationPolicy policyToApply;

  public OperationExecutionParams getOperationExecutionParams() {
    return operationExecutionParams;
  }

  public void setOperationExecutionParams(Optional<ConfigurationInstance> configuration, Map<String, Object> parameters,
                                          CoreEvent operationEvent, ExecutorCallback callback) {
    this.operationExecutionParams = new OperationExecutionParams(configuration, parameters, operationEvent, callback);
  }

  public Function<Context, Context> getInnerChainSubscriberContextMapping() {
    return innerChainSubscriberContextMapping;
  }

  public void setInnerChainSubscriberContextMapping(Function<Context, Context> innerChainSubscriberContextMapping) {
    this.innerChainSubscriberContextMapping = innerChainSubscriberContextMapping;
  }

  public Optional<ConfigurationInstance> getConfiguration() {
    return configuration;
  }

  public void setConfiguration(Optional<ConfigurationInstance> configuration) {
    this.configuration = configuration;
  }

  public Map<String, Object> getResolutionResult() {
    return resolutionResult;
  }

  public void setResolutionResult(Map<String, Object> resolutionResult) {
    this.resolutionResult = resolutionResult;
  }

  public OperationPolicy getPolicyToApply() {
    return policyToApply;
  }

  public void setPolicyToApply(OperationPolicy policyToApply) {
    this.policyToApply = policyToApply;
  }

  @Override
  public SdkInternalContext copy() {
    return this;
  }

  public static final class OperationExecutionParams {

    private final Optional<ConfigurationInstance> configuration;
    private final Map<String, Object> parameters;
    private final CoreEvent operationEvent;
    private final ExecutorCallback callback;

    public OperationExecutionParams(Optional<ConfigurationInstance> configuration, Map<String, Object> parameters,
                                    CoreEvent operationEvent, ExecutorCallback callback) {
      this.configuration = configuration;
      this.parameters = parameters;
      this.operationEvent = operationEvent;
      this.callback = callback;
    }

    public Optional<ConfigurationInstance> getConfiguration() {
      return configuration;
    }

    public Map<String, Object> getParameters() {
      return parameters;
    }

    public CoreEvent getOperationEvent() {
      return operationEvent;
    }

    public ExecutorCallback getCallback() {
      return callback;
    }

  }


}
