/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class UninitializedOperationMessageProcessor extends OperationMessageProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(UninitializedOperationMessageProcessor.class.getName());

  private OperationMessageProcessor operationMessageProcessorDelegate;
  private Map<String, ValueResolver> resolvedParameters;

  public UninitializedOperationMessageProcessor(ExtensionModel extension,
                                                OperationModel operation,
                                                PolicyManager policyManager,
                                                Registry registry,
                                                OperationMessageProcessor operationMessageProcessorDelegate,
                                                Map<String, ValueResolver> resolvedParameters,
                                                MuleContext muleContext,
                                                ConfigurationProvider configurationProvider) {

    super(extension,
          operation,
          configurationProvider,
          operationMessageProcessorDelegate.target,
          operationMessageProcessorDelegate.targetValue,
          operationMessageProcessorDelegate.resolverSet,
          null,
          operationMessageProcessorDelegate.retryPolicyTemplate,
          muleContext.getExtensionManager(),
          policyManager,
          registry.lookupByType(ReflectionCache.class).get());

    this.operationMessageProcessorDelegate = operationMessageProcessorDelegate;
    this.resolvedParameters = resolvedParameters;
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    //Do nothing
  }

  @Override
  protected ExecutionMediator createExecutionMediator() {
    return operationMessageProcessorDelegate.createExecutionMediator();
  }

  @Override
  protected void validateOperationConfiguration(ConfigurationProvider configurationProvider) {
    operationMessageProcessorDelegate.validateOperationConfiguration(configurationProvider);
  }

  @Override
  protected Mono<CoreEvent> doProcess(CoreEvent event, ExecutionContextAdapter<OperationModel> operationContext) {
    return operationMessageProcessorDelegate.doProcess(event, operationContext);
  }

  @Override protected ReturnDelegate createReturnDelegate() {
    return operationMessageProcessorDelegate.createReturnDelegate();
  }

  @Override protected TargetReturnDelegate getTargetReturnDelegate() {
    return operationMessageProcessorDelegate.getTargetReturnDelegate();
  }

  @Override protected ValueReturnDelegate getValueReturnDelegate() {
    return operationMessageProcessorDelegate.getValueReturnDelegate();
  }

  @Override protected boolean isTargetPresent() {
    return operationMessageProcessorDelegate.isTargetPresent();
  }

  @Override protected Optional<String> getTarget() {
    return operationMessageProcessorDelegate.getTarget();
  }

  @Override public void doStart() throws MuleException {
    operationMessageProcessorDelegate.doStart();
  }

  @Override
  public void doDispose() {
    operationMessageProcessorDelegate.doDispose();
  }

  @Override protected ParameterValueResolver getParameterValueResolver() {
    return operationMessageProcessorDelegate.getParameterValueResolver();
  }

  @Override public void resolveParameters(CoreEvent.Builder eventBuilder,
                                          BiConsumer<Map<String, Supplier<Object>>, ExecutionContext> afterConfigurer)
    throws MuleException {
    operationMessageProcessorDelegate.resolveParameters(eventBuilder, afterConfigurer);
  }

  @Override public void disposeResolvedParameters(ExecutionContext<OperationModel> executionContext) {
    operationMessageProcessorDelegate.disposeResolvedParameters(executionContext);
  }

  @Override public void doStop() throws MuleException {
    operationMessageProcessorDelegate.doStop();
  }

  @Override
  protected Map<String, Object> getResolutionResult(CoreEvent event, Optional<ConfigurationInstance> configuration)
    throws MuleException {

    ValueResolvingContext valueResolvingContext = ValueResolvingContext.builder(event).build();

    Object config = resolverSet.resolve(valueResolvingContext).asMap().get(CONFIG_ATTRIBUTE_NAME);

    Map<String, Object> resolvedParams = resolvedParameters.entrySet().stream()
      .filter(e -> !e.getKey().equals(CONFIG_ATTRIBUTE_NAME))
      .collect(toMap(entry -> entry.getKey(), entry -> {
        try {
          return entry.getValue().resolve(valueResolvingContext);
        } catch (MuleException e) {
          LOGGER.error("Could not resolve value [" + entry.getKey() + "] value: " + e.getMessage(), e);
          return null;
        }
      }));

    ImmutableMap.Builder<String, Object> params = ImmutableMap.<String, Object>builder().putAll(resolvedParams);
    if (config != null) {
      params.put(CONFIG_ATTRIBUTE_NAME, config);
    }
    return params.build();
  }
}
