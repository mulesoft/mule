/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.mule.runtime.core.internal.policy.PolicyPointcutParametersManager.POLICY_SOURCE_POINTCUT_PARAMETERS;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.process;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyProvider;
import org.mule.runtime.core.api.policy.PolicyStateHandler;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.policy.api.OperationPolicyPointcutParametersFactory;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.runtime.policy.api.SourcePolicyPointcutParametersFactory;

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

  @Inject
  private MuleContext muleContext;

  @Inject
  private PolicyStateHandler policyStateHandler;

  private final Cache<Pair<ComponentIdentifier, PolicyPointcutParameters>, SourcePolicy> sourcePolicyInstances =
      Caffeine.newBuilder().build();
  private final Cache<Pair<ComponentIdentifier, PolicyPointcutParameters>, OperationPolicy> operationPolicyInstances =
      Caffeine.newBuilder().build();

  private PolicyProvider policyProvider;
  private OperationPolicyProcessorFactory operationPolicyProcessorFactory;
  private SourcePolicyProcessorFactory sourcePolicyProcessorFactory;

  private PolicyPointcutParametersManager policyPointcutParametersManager;

  @Override
  public SourcePolicy createSourcePolicyInstance(Component source, CoreEvent sourceEvent,
                                                 MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
    final PolicyPointcutParameters sourcePointcutParameters = (PolicyPointcutParameters) ((InternalEvent) sourceEvent)
        .getInternalParameters().get(POLICY_SOURCE_POINTCUT_PARAMETERS);

    final ComponentIdentifier sourceIdentifier = source.getLocation().getComponentIdentifier().getIdentifier();
    final Pair<ComponentIdentifier, PolicyPointcutParameters> policyKey = new Pair<>(sourceIdentifier, sourcePointcutParameters);

    final SourcePolicy policy = sourcePolicyInstances.getIfPresent(policyKey);
    if (policy != null) {
      return policy;
    }

    return sourcePolicyInstances.get(policyKey, k -> {
      List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(sourcePointcutParameters);
      if (parameterizedPolicies.isEmpty()) {
        return (event, flowExecutionProcessor, respParamProcessor) -> from(process(event, flowExecutionProcessor))
            .map(flowExecutionResult -> right(SourcePolicyFailureResult.class,
                                              new SourcePolicySuccessResult(flowExecutionResult,
                                                                            () -> respParamProcessor
                                                                                .getSuccessfulExecutionResponseParametersFunction()
                                                                                .apply(flowExecutionResult),
                                                                            respParamProcessor)))
            .onErrorResume(MessagingException.class, messagingException -> {
              return just(left(new SourcePolicyFailureResult(messagingException, () -> respParamProcessor
                  .getFailedExecutionResponseParametersFunction()
                  .apply(messagingException.getEvent()))));
            });
      } else {
        return new CompositeSourcePolicy(parameterizedPolicies,
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
        return (operationEvent, operationExecutionFunction, opParamProcessor) -> operationExecutionFunction
            .execute(opParamProcessor.getOperationParameters(),
                     operationEvent);
      }
      return new CompositeOperationPolicy(parameterizedPolicies,
                                          lookupOperationParametersTransformer(operationIdentifier),
                                          operationPolicyProcessorFactory);
    });
  }

  private Optional<OperationPolicyParametersTransformer> lookupOperationParametersTransformer(ComponentIdentifier componentIdentifier) {
    MuleRegistry registry = ((MuleContextWithRegistry) muleContext).getRegistry();

    return registry.lookupObjects(OperationPolicyParametersTransformer.class).stream()
        .filter(policyOperationParametersTransformer -> policyOperationParametersTransformer.supports(componentIdentifier))
        .findAny();
  }

  private Optional<SourcePolicyParametersTransformer> lookupSourceParametersTransformer(ComponentIdentifier componentIdentifier) {
    MuleRegistry registry = ((MuleContextWithRegistry) muleContext).getRegistry();

    return registry.lookupObjects(SourcePolicyParametersTransformer.class).stream()
        .filter(policyOperationParametersTransformer -> policyOperationParametersTransformer.supports(componentIdentifier))
        .findAny();
  }

  @Override
  public void initialise() throws InitialisationException {
    operationPolicyProcessorFactory = new DefaultOperationPolicyProcessorFactory(policyStateHandler);
    sourcePolicyProcessorFactory = new DefaultSourcePolicyProcessorFactory(policyStateHandler);
    MuleRegistry registry = ((MuleContextWithRegistry) muleContext).getRegistry();
    policyProvider = registry.lookupLocalObjects(PolicyProvider.class).stream().findFirst().orElse(new NullPolicyProvider());
    policyProvider.onPoliciesChanged(() -> {
      sourcePolicyInstances.invalidateAll();
      operationPolicyInstances.invalidateAll();
    });

    policyPointcutParametersManager =
        new PolicyPointcutParametersManager(registry.lookupObjects(SourcePolicyPointcutParametersFactory.class),
                                            registry.lookupObjects(OperationPolicyPointcutParametersFactory.class));
  }

  @Override
  public void disposePoliciesResources(String executionIdentifier) {
    policyStateHandler.destroyState(executionIdentifier);
  }

  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
}
