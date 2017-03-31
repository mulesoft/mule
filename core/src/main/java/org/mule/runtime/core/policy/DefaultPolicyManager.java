/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import static java.util.Collections.emptyList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.functional.Either.right;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.exception.MessagingException;

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
  public SourcePolicy createSourcePolicyInstance(ComponentIdentifier sourceIdentifier, Event sourceEvent,
                                                 Processor flowExecutionProcessor,
                                                 MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
    PolicyPointcutParameters sourcePointcutParameters = createSourcePointcutParameters(sourceIdentifier, sourceEvent);
    List<Policy> parameterizedPolicies = policyProvider.findSourceParameterizedPolicies(sourcePointcutParameters);
    if (parameterizedPolicies.isEmpty()) {
      return event -> {
        try {
          Event flowExecutionResult = flowExecutionProcessor.process(sourceEvent);

          // TODO MULE-11141 - This is the case of a filtered flow. This will eventually go away.
          if (flowExecutionResult == null) {
            flowExecutionResult =
                Event.builder(sourceEvent).message(of(null)).build();
          }

          return right(new SuccessSourcePolicyResult(flowExecutionResult,
                                                     messageSourceResponseParametersProcessor
                                                         .getSuccessfulExecutionResponseParametersFunction()
                                                         .apply(flowExecutionResult),
                                                     messageSourceResponseParametersProcessor));
        } catch (Exception e) {
          MessagingException messagingException =
              e instanceof MessagingException ? (MessagingException) e : new MessagingException(event, e, flowExecutionProcessor);
          return Either.left(new FailureSourcePolicyResult(messagingException, messageSourceResponseParametersProcessor
              .getFailedExecutionResponseParametersFunction()
              .apply(messagingException.getEvent())));
        }
      };
    }
    return new CompositeSourcePolicy(parameterizedPolicies,
                                     lookupSourceParametersTransformer(sourceIdentifier),
                                     sourcePolicyProcessorFactory, flowExecutionProcessor,
                                     messageSourceResponseParametersProcessor);
  }

  @Override
  public OperationPolicy createOperationPolicy(ComponentIdentifier operationIdentifier, Event event,
                                               Map<String, Object> operationParameters,
                                               OperationExecutionFunction operationExecutionFunction) {

    PolicyPointcutParameters operationPointcutParameters =
        createOperationPointcutParameters(operationIdentifier, operationParameters, event.getContext().getOriginatingFlowName());
    List<Policy> parameterizedPolicies = policyProvider.findOperationParameterizedPolicies(operationPointcutParameters);
    if (parameterizedPolicies.isEmpty()) {
      return (operationEvent) -> operationExecutionFunction.execute(operationParameters, operationEvent);
    }
    return new CompositeOperationPolicy(parameterizedPolicies, lookupOperationParametersTransformer(operationIdentifier),
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

  private PolicyPointcutParameters createSourcePointcutParameters(ComponentIdentifier sourceIdentifier,
                                                                  Event sourceEvent) {
    return createPointcutParameters(sourceIdentifier, sourceEvent.getContext().getOriginatingFlowName(),
                                    SourcePolicyPointcutParametersFactory.class, sourcePointcutFactories,
                                    factory -> factory.supportsSourceIdentifier(sourceIdentifier),
                                    factory -> factory.createPolicyPointcutParameters(sourceEvent.getContext()
                                        .getOriginatingFlowName(), sourceIdentifier, sourceEvent.getMessage().getAttributes()));
  }

  private PolicyPointcutParameters createOperationPointcutParameters(ComponentIdentifier operationIdentifier,
                                                                     Map<String, Object> operationParameters,
                                                                     String originatingFlowName) {
    return createPointcutParameters(operationIdentifier, originatingFlowName, OperationPolicyPointcutParametersFactory.class,
                                    operationPointcutFactories,
                                    factory -> factory.supportsOperationIdentifier(operationIdentifier),
                                    factory -> factory.createPolicyPointcutParameters(originatingFlowName, operationIdentifier,
                                                                                      operationParameters));
  }

  private <T> PolicyPointcutParameters createPointcutParameters(ComponentIdentifier componentIdentifier, String flowName,
                                                                Class<T> factoryType, Collection<T> factories,
                                                                Predicate<T> factoryFilter,
                                                                Function<T, PolicyPointcutParameters> policyPointcutParametersCreationFunction) {
    List<T> policyPointcutParametersFactories = factories.stream()
        .filter(factoryFilter)
        .collect(Collectors.toList());
    if (policyPointcutParametersFactories.size() > 1) {
      return throwMoreThanOneFactoryFoundException(componentIdentifier, factoryType);
    }
    if (policyPointcutParametersFactories.isEmpty()) {
      return new PolicyPointcutParameters(flowName, componentIdentifier);
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

}
