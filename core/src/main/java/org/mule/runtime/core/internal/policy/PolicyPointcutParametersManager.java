/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.policy.api.OperationPolicyPointcutParametersFactory;
import org.mule.runtime.policy.api.OperationPolicyPointcutParametersParameters;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.runtime.policy.api.SourcePolicyPointcutParametersFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Responsible for the creation of {@link PolicyPointcutParameters} for both source and operation policies
 */
public class PolicyPointcutParametersManager {

  private final Collection<SourcePolicyPointcutParametersFactory> sourcePointcutFactories;
  private final Collection<OperationPolicyPointcutParametersFactory> operationPointcutFactories;

  private final Map<String, PolicyPointcutParameters> sourceParametersMap;

  public PolicyPointcutParametersManager(Collection<SourcePolicyPointcutParametersFactory> sourcePointcutFactories,
                                         Collection<OperationPolicyPointcutParametersFactory> operationPointcutFactories) {
    this.sourcePointcutFactories = sourcePointcutFactories;
    this.operationPointcutFactories = operationPointcutFactories;
    this.sourceParametersMap = new ConcurrentHashMap<>();
  }

  /**
   * Creates {@link PolicyPointcutParameters} for a specific source. The created parameters is also stored so it can be used in
   * case a matching policy also defines an operation part.
   * 
   * @param source the source component to which policies will be applied
   * @param event the event which will execute the source policies
   * @return the created {@link PolicyPointcutParameters}
   */
  public PolicyPointcutParameters createSourcePointcutParameters(Component source,
                                                                 CoreEvent event) {
    ComponentIdentifier sourceIdentifier = source.getLocation().getComponentIdentifier().getIdentifier();

    PolicyPointcutParameters sourcePointcutParameters =
        createPointcutParameters(source,
                                 SourcePolicyPointcutParametersFactory.class,
                                 sourcePointcutFactories,
                                 factory -> factory.supportsSourceIdentifier(sourceIdentifier),
                                 factory -> factory.createPolicyPointcutParameters(source, event.getMessage().getAttributes()))
                                     .orElse(new PolicyPointcutParameters(source));

    String correlationId = event.getContext().getCorrelationId();
    sourceParametersMap.put(correlationId, sourcePointcutParameters);
    ((BaseEventContext) event.getContext()).getRootContext().onTerminated((e, t) -> sourceParametersMap.remove(correlationId));

    return sourcePointcutParameters;
  }

  /**
   * Creates {@link PolicyPointcutParameters} for a specific operation. Stored parameters from the source are included in the
   * newly created parameters to be able to correlate parameters from both source and operation.
   * 
   * @param operation the operation component to which policies will be applied
   * @param event the event which will execute the operation policies
   * @param operationParameters a map containing the parameters of the operation
   * @return the created {@link PolicyPointcutParameters}
   */
  public PolicyPointcutParameters createOperationPointcutParameters(Component operation,
                                                                    CoreEvent event,
                                                                    Map<String, Object> operationParameters) {
    ComponentIdentifier operationIdentifier = operation.getLocation().getComponentIdentifier().getIdentifier();

    PolicyPointcutParameters sourceParameters = sourceParametersMap.get(event.getContext().getCorrelationId());

    OperationPolicyPointcutParametersParameters parameters =
        new OperationPolicyPointcutParametersParameters(operation, operationParameters, sourceParameters);

    Function<OperationPolicyPointcutParametersFactory, PolicyPointcutParameters> creationFunction =
        factory -> {
          try {
            return factory.createPolicyPointcutParameters(parameters);
          } catch (AbstractMethodError error) {
            return factory.createPolicyPointcutParameters(parameters.getOperation(), parameters.getOperationParameters());
          }
        };

    return createPointcutParameters(operation,
                                    OperationPolicyPointcutParametersFactory.class,
                                    operationPointcutFactories,
                                    factory -> factory.supportsOperationIdentifier(operationIdentifier),
                                    creationFunction)
                                        .orElse(new PolicyPointcutParameters(operation, sourceParameters));
  }

  private <T> Optional<PolicyPointcutParameters> createPointcutParameters(Component component, Class<T> factoryType,
                                                                          Collection<T> factories, Predicate<T> factoryFilter,
                                                                          Function<T, PolicyPointcutParameters> policyPointcutParametersCreationFunction) {

    T found = null;

    for (T factory : factories) {
      if (factoryFilter.test(factory)) {
        if (found != null) {
          throwMoreThanOneFactoryFoundException(component.getLocation().getComponentIdentifier().getIdentifier(), factoryType);
        }
        found = factory;
      }
    }

    return found != null ? of(policyPointcutParametersCreationFunction.apply(found)) : empty();
  }

  private void throwMoreThanOneFactoryFoundException(ComponentIdentifier sourceIdentifier, Class factoryClass) {
    throw new MuleRuntimeException(createStaticMessage(format(
                                                              "More than one %s for component %s was found. There should be only one.",
                                                              factoryClass.getName(), sourceIdentifier)));
  }
}
