/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Collections.emptyList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.functional.Either.right;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyProvider;
import org.mule.runtime.core.api.policy.PolicyStateHandler;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.policy.api.OperationPolicyPointcutParametersFactory;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.runtime.policy.api.SourcePolicyPointcutParametersFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

  private Collection<OperationPolicyParametersTransformer> operationPolicyParametersTransformerCollection = emptyList();
  private Collection<SourcePolicyParametersTransformer> sourcePolicyParametersTransformerCollection = emptyList();
  private Collection<SourcePolicyPointcutParametersFactory> sourcePointcutFactories = emptyList();
  private Collection<OperationPolicyPointcutParametersFactory> operationPointcutFactories = emptyList();
  private PolicyProvider policyProvider;
  private OperationPolicyProcessorFactory operationPolicyProcessorFactory;
  private SourcePolicyProcessorFactory sourcePolicyProcessorFactory;

  @Override
  public SourcePolicy createSourcePolicyInstance(ComponentLocation sourceLocation, Event sourceEvent,
                                                 Processor flowExecutionProcessor,
                                                 MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
    PolicyPointcutParameters sourcePointcutParameters = createSourcePointcutParameters(sourceLocation, sourceEvent);
    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(sourcePointcutParameters);
    if (parameterizedPolicies.isEmpty()) {
      return event -> just(sourceEvent).transform(flowExecutionProcessor)
          .defaultIfEmpty(Event.builder(sourceEvent).message(of(null)).build())
          .<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>>map(flowExecutionResult -> right(new SourcePolicySuccessResult(flowExecutionResult,
                                                                                                                                        () -> messageSourceResponseParametersProcessor
                                                                                                                                            .getSuccessfulExecutionResponseParametersFunction()
                                                                                                                                            .apply(flowExecutionResult),
                                                                                                                                        messageSourceResponseParametersProcessor)))
          .onErrorResume(Exception.class, e -> {
            MessagingException messagingException =
                e instanceof MessagingException ? (MessagingException) e
                    : new MessagingException(event, e, flowExecutionProcessor);
            return just(Either
                .left(new SourcePolicyFailureResult(messagingException, () -> messageSourceResponseParametersProcessor
                    .getFailedExecutionResponseParametersFunction()
                    .apply(messagingException.getEvent()))));
          });
    }
    return new CompositeSourcePolicy(parameterizedPolicies,
                                     lookupSourceParametersTransformer(sourceLocation.getComponentIdentifier().getIdentifier()),
                                     sourcePolicyProcessorFactory, flowExecutionProcessor,
                                     messageSourceResponseParametersProcessor);
  }

  @Override
  public OperationPolicy createOperationPolicy(ComponentLocation operationLocation, Event event,
                                               Map<String, Object> operationParameters,
                                               OperationExecutionFunction operationExecutionFunction) {

    PolicyPointcutParameters operationPointcutParameters =
        createOperationPointcutParameters(operationLocation, operationParameters);
    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(operationPointcutParameters);
    if (parameterizedPolicies.isEmpty()) {
      return (operationEvent) -> operationExecutionFunction.execute(operationParameters, operationEvent);
    }
    return new CompositeOperationPolicy(parameterizedPolicies,
                                        lookupOperationParametersTransformer(operationLocation.getComponentIdentifier()
                                            .getIdentifier()),
                                        operationPolicyProcessorFactory, () -> operationParameters, operationExecutionFunction);
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
    try {
      operationPolicyProcessorFactory = new DefaultOperationPolicyProcessorFactory(policyStateHandler);
      sourcePolicyProcessorFactory = new DefaultSourcePolicyProcessorFactory(policyStateHandler);
      policyProvider = muleContext.getRegistry().lookupObject(PolicyProvider.class);
      if (policyProvider == null) {
        policyProvider = new NullPolicyProvider();
      }
      sourcePolicyParametersTransformerCollection =
          muleContext.getRegistry().lookupObjects(SourcePolicyParametersTransformer.class);
      operationPolicyParametersTransformerCollection =
          muleContext.getRegistry().lookupObjects(OperationPolicyParametersTransformer.class);
      sourcePointcutFactories = muleContext.getRegistry().lookupObjects(SourcePolicyPointcutParametersFactory.class);
      operationPointcutFactories = muleContext.getRegistry().lookupObjects(OperationPolicyPointcutParametersFactory.class);
    } catch (RegistrationException e) {
      throw new InitialisationException(e, this);
    }
  }

  private PolicyPointcutParameters createSourcePointcutParameters(ComponentLocation sourceLocation,
                                                                  Event sourceEvent) {
    return createPointcutParameters(sourceLocation, SourcePolicyPointcutParametersFactory.class, sourcePointcutFactories,
                                    factory -> factory
                                        .supportsSourceIdentifier(sourceLocation.getComponentIdentifier().getIdentifier()),
                                    factory -> factory.createPolicyPointcutParameters(sourceLocation,
                                                                                      sourceEvent.getMessage().getAttributes()));
  }

  private PolicyPointcutParameters createOperationPointcutParameters(ComponentLocation operationLocation,
                                                                     Map<String, Object> operationParameters) {
    return createPointcutParameters(operationLocation, OperationPolicyPointcutParametersFactory.class, operationPointcutFactories,
                                    factory -> factory
                                        .supportsOperationIdentifier(operationLocation.getComponentIdentifier().getIdentifier()),
                                    factory -> factory.createPolicyPointcutParameters(operationLocation, operationParameters));
  }

  private <T> PolicyPointcutParameters createPointcutParameters(ComponentLocation location, Class<T> factoryType,
                                                                Collection<T> factories, Predicate<T> factoryFilter,
                                                                Function<T, PolicyPointcutParameters> policyPointcutParametersCreationFunction) {
    List<T> policyPointcutParametersFactories = factories.stream()
        .filter(factoryFilter)
        .collect(Collectors.toList());
    if (policyPointcutParametersFactories.size() > 1) {
      return throwMoreThanOneFactoryFoundException(location.getComponentIdentifier().getIdentifier(), factoryType);
    }
    if (policyPointcutParametersFactories.isEmpty()) {
      return new PolicyPointcutParameters(location);
    }
    return policyPointcutParametersCreationFunction.apply(policyPointcutParametersFactories.get(0));
  }

  private PolicyPointcutParameters throwMoreThanOneFactoryFoundException(ComponentIdentifier sourceIdentifier,
                                                                         Class factoryClass) {
    throw new MuleRuntimeException(createStaticMessage(String.format(
                                                                     "More than one %s for component %s was found. There should be only one.",
                                                                     factoryClass.getName(), sourceIdentifier)));
  }

  @Override
  public void disposePoliciesResources(String executionIdentifier) {
    policyStateHandler.destroyState(executionIdentifier);
  }


  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
}
