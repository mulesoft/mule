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

  private final Map<ComponentLocation, LocationSpecificSdkInternalContext> locationSpecificContext = new SmallMap<>();

  public void clearContextForLocation(ComponentLocation location) {
    locationSpecificContext.remove(location);
  }

  public void setOperationExecutionParams(ComponentLocation location, Optional<ConfigurationInstance> configuration,
                                          Map<String, Object> parameters, CoreEvent operationEvent, ExecutorCallback callback) {

    final LocationSpecificSdkInternalContext ctx = new LocationSpecificSdkInternalContext();
    ctx.setOperationExecutionParams(configuration, parameters, operationEvent, callback);
    locationSpecificContext.put(location, ctx);
  }

  public OperationExecutionParams getOperationExecutionParams(ComponentLocation location) {
    return locationSpecificContext.get(location).operationExecutionParams;
  }

  public Function<Context, Context> getInnerChainSubscriberContextMapping(ComponentLocation location) {
    return locationSpecificContext.get(location).getInnerChainSubscriberContextMapping();
  }

  public void setInnerChainSubscriberContextMapping(ComponentLocation location,
                                                    Function<Context, Context> innerChainSubscriberContextMapping) {
    locationSpecificContext.get(location).setInnerChainSubscriberContextMapping(innerChainSubscriberContextMapping);
  }

  public Optional<ConfigurationInstance> getConfiguration(ComponentLocation location) {
    return locationSpecificContext.get(location).getConfiguration();
  }

  public void setConfiguration(ComponentLocation location, Optional<ConfigurationInstance> configuration) {
    locationSpecificContext.get(location).setConfiguration(configuration);
  }

  public Map<String, Object> getResolutionResult(ComponentLocation location) {
    return locationSpecificContext.get(location).getResolutionResult();
  }

  public void setResolutionResult(ComponentLocation location, Map<String, Object> resolutionResult) {
    locationSpecificContext.get(location).setResolutionResult(resolutionResult);
  }

  public OperationPolicy getPolicyToApply(ComponentLocation location) {
    return locationSpecificContext.get(location).getPolicyToApply();
  }

  public void setPolicyToApply(ComponentLocation location, OperationPolicy policyToApply) {
    locationSpecificContext.get(location).setPolicyToApply(policyToApply);
  }

  /**
   * @return {@code true} if the policy to be applied is a no-op, {@code false} if a policy is actually applied.
   */
  public boolean isNoPolicyOperation() {
    return DefaultPolicyManager.isNoPolicyOperation(getPolicyToApply());
  }

  @Override
  public SdkInternalContext copy() {
    return this;
  }

  public static final class LocationSpecificSdkInternalContext {

    private OperationExecutionParams operationExecutionParams;

    private Function<Context, Context> innerChainSubscriberContextMapping = identity();

    private Optional<ConfigurationInstance> configuration;

    private Map<String, Object> resolutionResult;

    private OperationPolicy policyToApply;

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
