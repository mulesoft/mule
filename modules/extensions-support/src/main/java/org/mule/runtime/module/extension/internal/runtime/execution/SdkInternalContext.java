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
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.policy.DefaultPolicyManager;
import org.mule.runtime.core.internal.policy.OperationPolicy;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import reactor.util.context.Context;

/**
 * Contains internal context handled by SDK operations.
 *
 * @since 4.3
 */
public class SdkInternalContext implements EventInternalContext<SdkInternalContext> {

  /**
   * Extracts an instance stored in the given {@code event}
   *
   * @param event
   * @return an {@link SdkInternalContext} or {@code null} if none was set on the event
   */
  public static SdkInternalContext from(CoreEvent event) {
    return (SdkInternalContext) ((InternalEvent) event).<SdkInternalContext>getSdkInternalContext();
  }

  /*
   * SDK components may be nested within each other, so some of the context must be kept separately for the component it belongs
   * to.
   */
  private final Deque<LocationSpecificSdkInternalContext> locationSpecificContext = new ArrayDeque<>();

  private Function<Context, Context> innerChainSubscriberContextMapping = identity();

  public void popContext() {
    locationSpecificContext.pop();
  }

  public void pushContext() {
    locationSpecificContext.push(new LocationSpecificSdkInternalContext());
  }

  /**
   * For a given location, this method has to always be called first, so the context for this location is properly initialized.
   *
   * @param configuration the configuration of the operation.
   */
  public void setConfiguration(Optional<ConfigurationInstance> configuration) {
    locationSpecificContext.peek().setConfiguration(configuration);
  }

  public Optional<ConfigurationInstance> getConfiguration() {
    return locationSpecificContext.peek().getConfiguration();
  }

  public void setOperationExecutionParams(Optional<ConfigurationInstance> configuration,
                                          Map<String, Object> parameters, CoreEvent operationEvent, ExecutorCallback callback) {
    locationSpecificContext.peek().setOperationExecutionParams(configuration, parameters, operationEvent,
                                                               callback);
  }

  public OperationExecutionParams getOperationExecutionParams() {
    return locationSpecificContext.peek().getOperationExecutionParams();
  }

  public Map<String, Object> getResolutionResult() {
    return locationSpecificContext.peek().getResolutionResult();
  }

  public void setResolutionResult(Map<String, Object> resolutionResult) {
    locationSpecificContext.peek().setResolutionResult(resolutionResult);
  }

  public OperationPolicy getPolicyToApply() {
    return locationSpecificContext.peek().getPolicyToApply();
  }

  public void setPolicyToApply(OperationPolicy policyToApply) {
    locationSpecificContext.peek().setPolicyToApply(policyToApply);
  }

  /**
   * @return {@code true} if the policy to be applied is a no-op, {@code false} if a policy is actually applied.
   */
  public boolean isNoPolicyOperation() {
    return DefaultPolicyManager.isNoPolicyOperation(getPolicyToApply());
  }

  public Function<Context, Context> getInnerChainSubscriberContextMapping() {
    return innerChainSubscriberContextMapping;
  }

  public void setInnerChainSubscriberContextMapping(Function<Context, Context> innerChainSubscriberContextMapping) {
    this.innerChainSubscriberContextMapping = innerChainSubscriberContextMapping;
  }

  @Override
  public SdkInternalContext copy() {
    return this;
  }

  public static final class LocationSpecificSdkInternalContext {

    private OperationExecutionParams operationExecutionParams;

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
