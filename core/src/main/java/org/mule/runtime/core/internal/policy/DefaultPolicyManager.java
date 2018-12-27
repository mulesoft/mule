/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.internal.policy.PolicyPointcutParametersManager.POLICY_SOURCE_POINTCUT_PARAMETERS;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyProvider;
import org.mule.runtime.core.api.policy.PolicyStateHandler;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.policy.api.OperationPolicyPointcutParametersFactory;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.runtime.policy.api.SourcePolicyPointcutParametersFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

/**
 * Default implementation of {@link PolicyManager}.
 *
 * @since 4.0
 */
public class DefaultPolicyManager implements PolicyManager, Initialisable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPolicyManager.class);

  private static final OperationPolicy NO_POLICY_OPERATION =
      (operationEvent, operationExecutionFunction, opParamProcessor) -> operationExecutionFunction
          .execute(opParamProcessor.getOperationParameters(), operationEvent);

  @Inject
  private Registry registry;

  @Inject
  private PolicyStateHandler policyStateHandler;

  private final Cache<ComponentIdentifier, SourcePolicy> noPolicySourceInstances =
      Caffeine.newBuilder()
          .removalListener((key, value, cause) -> disposeIfNeeded(value, LOGGER))
          .build();
  private final Cache<Pair<ComponentIdentifier, PolicyPointcutParameters>, SourcePolicy> sourcePolicyInstances =
      Caffeine.newBuilder()
          .removalListener((key, value, cause) -> disposeIfNeeded(value, LOGGER))
          .build();
  private final Cache<Pair<ComponentIdentifier, PolicyPointcutParameters>, OperationPolicy> operationPolicyInstances =
      Caffeine.newBuilder()
          .removalListener((key, value, cause) -> disposeIfNeeded(value, LOGGER))
          .build();

  private PolicyProvider policyProvider;
  private OperationPolicyProcessorFactory operationPolicyProcessorFactory;
  private SourcePolicyProcessorFactory sourcePolicyProcessorFactory;

  private PolicyPointcutParametersManager policyPointcutParametersManager;

  @Override
  public SourcePolicy createSourcePolicyInstance(Component source, CoreEvent sourceEvent,
                                                 ReactiveProcessor flowExecutionProcessor,
                                                 MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
    final ComponentIdentifier sourceIdentifier = source.getLocation().getComponentIdentifier().getIdentifier();

    if (!policyProvider.isPoliciesAvailable()) {
      final SourcePolicy policy = noPolicySourceInstances.getIfPresent(sourceIdentifier);

      if (policy != null) {
        return policy;
      }

      return noPolicySourceInstances.get(sourceIdentifier, k -> new NoSourcePolicy(flowExecutionProcessor));
    }

    final PolicyPointcutParameters sourcePointcutParameters = ((InternalEvent) sourceEvent)
        .getInternalParameter(POLICY_SOURCE_POINTCUT_PARAMETERS);

    final Pair<ComponentIdentifier, PolicyPointcutParameters> policyKey = new Pair<>(sourceIdentifier, sourcePointcutParameters);

    final SourcePolicy policy = sourcePolicyInstances.getIfPresent(policyKey);
    if (policy != null) {
      return policy;
    }

    return sourcePolicyInstances.get(policyKey, k -> {
      List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(sourcePointcutParameters);
      if (parameterizedPolicies.isEmpty()) {
        return new NoSourcePolicy(flowExecutionProcessor);
      } else {
        return new CompositeSourcePolicy(parameterizedPolicies, flowExecutionProcessor,
                                         lookupSourceParametersTransformer(sourceIdentifier),
                                         sourcePolicyProcessorFactory);
      }
    });
  }

  @Override
  public PolicyPointcutParameters addSourcePointcutParametersIntoEvent(Component source, TypedValue<?> attributes,
                                                                       InternalEvent.Builder eventBuilder) {
    final PolicyPointcutParameters sourcePolicyParams =
        policyPointcutParametersManager.createSourcePointcutParameters(source, attributes);
    eventBuilder.addInternalParameter(POLICY_SOURCE_POINTCUT_PARAMETERS, sourcePolicyParams);
    return sourcePolicyParams;
  }

  @Override
  public OperationPolicy createOperationPolicy(Component operation, CoreEvent event,
                                               OperationParametersProcessor operationParameters) {
    if (!policyProvider.isPoliciesAvailable()) {
      return NO_POLICY_OPERATION;
    }

    PolicyPointcutParameters operationPointcutParameters =
        policyPointcutParametersManager.createOperationPointcutParameters(operation, event,
                                                                          operationParameters.getOperationParameters());

    final ComponentIdentifier operationIdentifier = operation.getLocation().getComponentIdentifier().getIdentifier();
    final Pair<ComponentIdentifier, PolicyPointcutParameters> policyKey =
        new Pair<>(operationIdentifier, operationPointcutParameters);

    final OperationPolicy policy = operationPolicyInstances.getIfPresent(policyKey);
    if (policy != null) {
      return policy;
    }

    return operationPolicyInstances.get(policyKey, k -> {
      List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(operationPointcutParameters);
      if (parameterizedPolicies.isEmpty()) {
        return NO_POLICY_OPERATION;
      } else {
        return new CompositeOperationPolicy(parameterizedPolicies,
                                            lookupOperationParametersTransformer(operationIdentifier),
                                            operationPolicyProcessorFactory);
      }
    });
  }

  private Optional<OperationPolicyParametersTransformer> lookupOperationParametersTransformer(ComponentIdentifier componentIdentifier) {
    return registry.lookupAllByType(OperationPolicyParametersTransformer.class).stream()
        .filter(policyOperationParametersTransformer -> policyOperationParametersTransformer.supports(componentIdentifier))
        .findAny();
  }

  private Optional<SourcePolicyParametersTransformer> lookupSourceParametersTransformer(ComponentIdentifier componentIdentifier) {
    return registry.lookupAllByType(SourcePolicyParametersTransformer.class).stream()
        .filter(policyOperationParametersTransformer -> policyOperationParametersTransformer.supports(componentIdentifier))
        .findAny();
  }

  @Override
  public void initialise() throws InitialisationException {
    operationPolicyProcessorFactory = new DefaultOperationPolicyProcessorFactory(policyStateHandler);
    sourcePolicyProcessorFactory = new DefaultSourcePolicyProcessorFactory(policyStateHandler);

    policyProvider = registry.lookupByType(PolicyProvider.class).orElse(new NullPolicyProvider());
    policyProvider.onPoliciesChanged(() -> {
      noPolicySourceInstances.invalidateAll();
      sourcePolicyInstances.invalidateAll();
      operationPolicyInstances.invalidateAll();
    });

    policyPointcutParametersManager =
        new PolicyPointcutParametersManager(registry.lookupAllByType(SourcePolicyPointcutParametersFactory.class),
                                            registry.lookupAllByType(OperationPolicyPointcutParametersFactory.class));
  }

  @Override
  public void disposePoliciesResources(String executionIdentifier) {
    policyStateHandler.destroyState(executionIdentifier);
  }

}
