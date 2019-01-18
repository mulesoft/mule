/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.process;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyProvider;
import org.mule.runtime.core.api.policy.PolicyStateHandler;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.policy.api.OperationPolicyPointcutParametersFactory;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.runtime.policy.api.SourcePolicyPointcutParametersFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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

  @Inject
  private PolicyNextChaining policyNextChaining;

  private final ConcurrentHashMap<ComponentIdentifier, Optional<SourcePolicyParametersTransformer>> sourceParametersTransformers =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<ComponentIdentifier, Optional<OperationPolicyParametersTransformer>> operationParametersTransformers =
      new ConcurrentHashMap<>();

  private PolicyProvider policyProvider;
  private OperationPolicyProcessorFactory operationPolicyProcessorFactory;
  private SourcePolicyProcessorFactory sourcePolicyProcessorFactory;

  private PolicyPointcutParametersManager policyPointcutParametersManager;

  @Override
  public SourcePolicy createSourcePolicyInstance(Component source, CoreEvent sourceEvent,
                                                 Processor flowExecutionProcessor,
                                                 MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {

    PolicyPointcutParameters sourcePointcutParameters =
        policyPointcutParametersManager.createSourcePointcutParameters(source, sourceEvent);

    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(sourcePointcutParameters);
    if (parameterizedPolicies.isEmpty()) {
      return (event, respParamProcessor) -> from(process(event, flowExecutionProcessor))
          .<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>>map(flowExecutionResult -> right(new SourcePolicySuccessResult(flowExecutionResult,
                                                                                                                                        () -> respParamProcessor
                                                                                                                                            .getSuccessfulExecutionResponseParametersFunction()
                                                                                                                                            .apply(flowExecutionResult),
                                                                                                                                        respParamProcessor)))
          .onErrorResume(Exception.class, e -> {
            MessagingException messagingException = e instanceof MessagingException ? (MessagingException) e
                : new MessagingException(event, e, (Component) flowExecutionProcessor);
            return just(left(new SourcePolicyFailureResult(messagingException, () -> respParamProcessor
                .getFailedExecutionResponseParametersFunction()
                .apply(messagingException.getEvent()))));
          });
    } else {
      return new CompositeSourcePolicy(parameterizedPolicies,
                                       lookupSourceParametersTransformer(source.getLocation().getComponentIdentifier()
                                           .getIdentifier()),
                                       sourcePolicyProcessorFactory, flowExecutionProcessor,
                                       messageSourceResponseParametersProcessor);
    }
  }

  @Override
  public OperationPolicy createOperationPolicy(Component operation, CoreEvent event,
                                               Map<String, Object> operationParameters,
                                               OperationExecutionFunction operationExecutionFunction) {

    PolicyPointcutParameters operationPointcutParameters =
        policyPointcutParametersManager.createOperationPointcutParameters(operation, event, operationParameters);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(operationPointcutParameters);
    if (parameterizedPolicies.isEmpty()) {
      return (operationEvent, opParamProcessor) -> operationExecutionFunction.execute(opParamProcessor.getOperationParameters(),
                                                                                      operationEvent);
    }
    return new CompositeOperationPolicy(parameterizedPolicies,
                                        lookupOperationParametersTransformer(operation.getLocation().getComponentIdentifier()
                                            .getIdentifier()),
                                        operationPolicyProcessorFactory, () -> operationParameters, operationExecutionFunction);
  }

  private Optional<OperationPolicyParametersTransformer> lookupOperationParametersTransformer(ComponentIdentifier componentIdentifier) {
    Optional<OperationPolicyParametersTransformer> cached = operationParametersTransformers.get(componentIdentifier);
    if (cached != null) {
      return cached;
    }

    MuleRegistry registry = ((MuleContextWithRegistries) muleContext).getRegistry();

    return operationParametersTransformers
        .computeIfAbsent(componentIdentifier, cId -> registry.lookupObjects(OperationPolicyParametersTransformer.class).stream()
            .filter(policyOperationParametersTransformer -> policyOperationParametersTransformer.supports(cId))
            .findAny());
  }

  private Optional<SourcePolicyParametersTransformer> lookupSourceParametersTransformer(ComponentIdentifier componentIdentifier) {
    Optional<SourcePolicyParametersTransformer> cached = sourceParametersTransformers.get(componentIdentifier);
    if (cached != null) {
      return cached;
    }

    MuleRegistry registry = ((MuleContextWithRegistries) muleContext).getRegistry();

    return sourceParametersTransformers
        .computeIfAbsent(componentIdentifier, cId -> registry.lookupObjects(SourcePolicyParametersTransformer.class).stream()
            .filter(policyOperationParametersTransformer -> policyOperationParametersTransformer.supports(cId))
            .findAny());
  }

  @Override
  public void initialise() throws InitialisationException {
    operationPolicyProcessorFactory = new DefaultOperationPolicyProcessorFactory(policyStateHandler, policyNextChaining);
    sourcePolicyProcessorFactory = new DefaultSourcePolicyProcessorFactory(policyStateHandler, policyNextChaining);
    MuleRegistry registry = ((MuleContextWithRegistries) muleContext).getRegistry();
    policyProvider = registry.lookupLocalObjects(PolicyProvider.class).stream().findFirst().orElse(new NullPolicyProvider());

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
