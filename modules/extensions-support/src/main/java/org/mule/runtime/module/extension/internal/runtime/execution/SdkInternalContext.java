/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.util.Collections.synchronizedMap;
import static java.util.Optional.empty;
import static java.util.function.Function.identity;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.event.InternalEvent;
import org.mule.runtime.core.internal.message.EventInternalContext;
import org.mule.runtime.core.internal.policy.DefaultPolicyManager;
import org.mule.runtime.core.internal.policy.OperationPolicy;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;

import reactor.util.context.Context;

/**
 * Contains internal context handled by SDK operations.
 *
 * @since 4.3
 */
public class SdkInternalContext implements EventInternalContext<SdkInternalContext> {

  private static final Logger LOGGER = getLogger(SdkInternalContext.class);

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
  // TODO MULE-18296 determine what implementation of thread safe map is better here after that fix.
  private final Map<Pair<ComponentLocation, String>, LocationSpecificSdkInternalContext> locationSpecificContext =
      synchronizedMap(new SmallMap<>());

  private Function<Context, Context> innerChainSubscriberContextMapping = identity();

  public void removeContext(ComponentLocation location, String eventId) {
    LOGGER.debug("Removing context at location - {} for event - {}", location != null ? location.getLocation() : "null", eventId);
    locationSpecificContext.remove(new Pair<>(location, eventId));
  }

  public void putContext(ComponentLocation location, String eventId) {
    final var locationString = location != null ? location.getLocation() : "null";
    LOGGER.debug("Adding new context at location - {} for event - {}", locationString, eventId);

    final var previousValue =
        locationSpecificContext.put(new Pair<>(location, eventId), new LocationSpecificSdkInternalContext());
    if (previousValue != null) {
      throw new IllegalStateException("Context at location - %s for event - %s already present"
          .formatted(locationString, eventId));
    }
  }

  /**
   * For a given location, this method has to always be called first, so the context for this location is properly initialized.
   *
   * @param location      the location of the operation that generated this context.
   * @param configuration the configuration of the operation.
   */
  public void setConfiguration(ComponentLocation location, String eventId,
                               Optional<ConfigurationInstance> configuration) {
    LOGGER.debug("Adding configuration at location - {} for event - {}", location != null ? location.getLocation() : "null",
                 eventId);
    locationSpecificContext.get(new Pair<>(location, eventId)).setConfiguration(configuration);
  }

  public Optional<ConfigurationInstance> getConfiguration(ComponentLocation location, String eventId) {
    return locationSpecificContext.get(new Pair<>(location, eventId)).getConfiguration();
  }

  public void setOperationExecutionParams(ComponentLocation location, String eventId,
                                          Optional<ConfigurationInstance> configuration,
                                          Map<String, Object> parameters, CoreEvent operationEvent, ExecutorCallback callback,
                                          ExecutionContextAdapter executionContextAdapter) {
    LOGGER.debug("Setting Operation Parameters at location - {} for event - {}",
                 location != null ? location.getLocation() : "null", eventId);
    locationSpecificContext.get(new Pair<>(location, eventId)).setOperationExecutionParams(configuration, parameters,
                                                                                           operationEvent,
                                                                                           callback, executionContextAdapter);
  }

  public OperationExecutionParams getOperationExecutionParams(ComponentLocation location, String eventId) {
    return locationSpecificContext.get(new Pair<>(location, eventId)).getOperationExecutionParams();
  }

  public Map<String, Object> getResolutionResult(ComponentLocation location, String eventId) {
    return locationSpecificContext.get(new Pair<>(location, eventId)).getResolutionResult();
  }

  public void setResolutionResult(ComponentLocation location, String eventId, Map<String, Object> resolutionResult) {
    locationSpecificContext.get(new Pair<>(location, eventId)).setResolutionResult(resolutionResult);
  }

  public OperationPolicy getPolicyToApply(ComponentLocation location, String eventId) {
    return locationSpecificContext.get(new Pair<>(location, eventId)).getPolicyToApply();
  }

  public void setPolicyToApply(ComponentLocation location, String eventId, OperationPolicy policyToApply) {
    locationSpecificContext.get(new Pair<>(location, eventId)).setPolicyToApply(policyToApply);
  }

  /**
   * @return {@code true} if the policy to be applied is a no-op, {@code false} if a policy is actually applied.
   */
  public boolean isNoPolicyOperation(ComponentLocation location, String eventId) {
    return DefaultPolicyManager.isNoPolicyOperation(getPolicyToApply(location, eventId));
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

    private Optional<ConfigurationInstance> configuration = empty();

    private Map<String, Object> resolutionResult;

    private OperationPolicy policyToApply;

    public OperationExecutionParams getOperationExecutionParams() {
      return operationExecutionParams;
    }

    public void setOperationExecutionParams(Optional<ConfigurationInstance> configuration, Map<String, Object> parameters,
                                            CoreEvent operationEvent, ExecutorCallback callback,
                                            ExecutionContextAdapter executionContextAdapter) {
      this.operationExecutionParams =
          new OperationExecutionParams(configuration, parameters, operationEvent, callback, executionContextAdapter);
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
    private final ExecutionContextAdapter executionContextAdapter;

    public OperationExecutionParams(Optional<ConfigurationInstance> configuration, Map<String, Object> parameters,
                                    CoreEvent operationEvent, ExecutorCallback callback,
                                    ExecutionContextAdapter executionContextAdapter) {
      this.configuration = configuration;
      this.parameters = parameters;
      this.operationEvent = operationEvent;
      this.callback = callback;
      this.executionContextAdapter = executionContextAdapter;
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

    public ExecutionContextAdapter getExecutionContextAdapter() {
      return executionContextAdapter;
    }
  }
}
