/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Collections.emptyList;
import static org.mule.runtime.api.message.Message.of;
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
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.policy.api.OperationPolicyPointcutParametersFactory;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.runtime.policy.api.SourcePolicyPointcutParametersFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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

  @Inject
  private StreamingManager streamingManager;

  private Collection<OperationPolicyParametersTransformer> operationPolicyParametersTransformerCollection = emptyList();
  private Collection<SourcePolicyParametersTransformer> sourcePolicyParametersTransformerCollection = emptyList();
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
      return event -> from(process(event, flowExecutionProcessor))
          .defaultIfEmpty(CoreEvent.builder(sourceEvent).message(of(null)).build())
          .<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>>map(flowExecutionResult -> right(new SourcePolicySuccessResult(flowExecutionResult,
                                                                                                                                        () -> messageSourceResponseParametersProcessor
                                                                                                                                            .getSuccessfulExecutionResponseParametersFunction()
                                                                                                                                            .apply(flowExecutionResult),
                                                                                                                                        messageSourceResponseParametersProcessor)))
          .onErrorResume(Exception.class, e -> {
            MessagingException messagingException = e instanceof MessagingException ? (MessagingException) e
                : new MessagingException(event, e, (Component) flowExecutionProcessor);
            return just(Either
                .left(new SourcePolicyFailureResult(messagingException, () -> messageSourceResponseParametersProcessor
                    .getFailedExecutionResponseParametersFunction()
                    .apply(messagingException.getEvent()))));
          });
    }
    return new CompositeSourcePolicy(parameterizedPolicies,
                                     lookupSourceParametersTransformer(source.getLocation().getComponentIdentifier()
                                         .getIdentifier()),
                                     sourcePolicyProcessorFactory, flowExecutionProcessor,
                                     messageSourceResponseParametersProcessor);
  }

  @Override
  public OperationPolicy createOperationPolicy(Component operation, CoreEvent event,
                                               Map<String, Object> operationParameters,
                                               OperationExecutionFunction operationExecutionFunction) {

    PolicyPointcutParameters operationPointcutParameters =
        policyPointcutParametersManager.createOperationPointcutParameters(operation, event, operationParameters);

    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(operationPointcutParameters);
    if (parameterizedPolicies.isEmpty()) {
      return (operationEvent) -> operationExecutionFunction.execute(operationParameters, operationEvent);
    }
    return new CompositeOperationPolicy(parameterizedPolicies,
                                        lookupOperationParametersTransformer(operation.getLocation().getComponentIdentifier()
                                            .getIdentifier()),
                                        operationPolicyProcessorFactory, () -> operationParameters, operationExecutionFunction,
                                        streamingManager);
  }

  private Optional<OperationPolicyParametersTransformer> lookupOperationParametersTransformer(ComponentIdentifier componentIdentifier) {
    return operationPolicyParametersTransformerCollection.stream()
        .filter(policyOperationParametersTransformer -> policyOperationParametersTransformer.supports(componentIdentifier))
        .findAny();
  }

  private Optional<SourcePolicyParametersTransformer> lookupSourceParametersTransformer(ComponentIdentifier componentIdentifier) {
    return sourcePolicyParametersTransformerCollection.stream()
        .filter(policyOperationParametersTransformer -> policyOperationParametersTransformer.supports(componentIdentifier))
        .findAny();
  }

  @Override
  public void initialise() throws InitialisationException {
    operationPolicyProcessorFactory = new DefaultOperationPolicyProcessorFactory(policyStateHandler);
    sourcePolicyProcessorFactory = new DefaultSourcePolicyProcessorFactory(policyStateHandler);
    MuleRegistry registry = ((MuleContextWithRegistry) muleContext).getRegistry();
    policyProvider = registry.lookupLocalObjects(PolicyProvider.class).stream().findFirst().orElse(new NullPolicyProvider());
    sourcePolicyParametersTransformerCollection = registry.lookupObjects(SourcePolicyParametersTransformer.class);
    operationPolicyParametersTransformerCollection = registry.lookupObjects(OperationPolicyParametersTransformer.class);
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
