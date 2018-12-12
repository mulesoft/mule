/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.policy.api.OperationPolicyPointcutParametersFactory;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.runtime.policy.api.SourcePolicyPointcutParametersFactory;

import java.util.Collection;
import java.util.Map;

/**
 * Responsible for the creation of {@link PolicyPointcutParameters} for both source and operation policies
 */
public class PolicyPointcutParametersManager {

  static final String POLICY_SOURCE_POINTCUT_PARAMETERS = "policy.sourcePointcutParameters";

  private final Collection<SourcePolicyPointcutParametersFactory> sourcePointcutFactories;
  private final Collection<OperationPolicyPointcutParametersFactory> operationPointcutFactories;

  public PolicyPointcutParametersManager(Collection<SourcePolicyPointcutParametersFactory> sourcePointcutFactories,
                                         Collection<OperationPolicyPointcutParametersFactory> operationPointcutFactories) {
    this.sourcePointcutFactories = sourcePointcutFactories;
    this.operationPointcutFactories = operationPointcutFactories;
  }

  /**
   * Creates {@link PolicyPointcutParameters} for a specific source. The created parameters is also stored so it can be used in
   * case a matching policy also defines an operation part.
   *
   * @param source the source component to which policies will be applied
   * @param event the event which will execute the source policies
   * @return the created {@link PolicyPointcutParameters}
   */
  public PolicyPointcutParameters createSourcePointcutParameters(Component source, TypedValue<?> attributes) {
    ComponentIdentifier sourceIdentifier = source.getLocation().getComponentIdentifier().getIdentifier();

    SourcePolicyPointcutParametersFactory found = null;
    for (SourcePolicyPointcutParametersFactory factory : sourcePointcutFactories) {
      if (factory.supportsSourceIdentifier(sourceIdentifier)) {
        if (found != null) {
          throwMoreThanOneFactoryFoundException(sourceIdentifier, SourcePolicyPointcutParametersFactory.class);
        }
        found = factory;
      }
    }

    return found != null ? found.createPolicyPointcutParameters(source, attributes)
        : new PolicyPointcutParameters(source);
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
  public PolicyPointcutParameters createOperationPointcutParameters(Component operation, CoreEvent event,
                                                                    Map<String, Object> operationParameters) {
    ComponentIdentifier operationIdentifier = operation.getLocation().getComponentIdentifier().getIdentifier();

    OperationPolicyPointcutParametersFactory found = null;
    for (OperationPolicyPointcutParametersFactory factory : operationPointcutFactories) {
      if (factory.supportsOperationIdentifier(operationIdentifier)) {
        if (found != null) {
          throwMoreThanOneFactoryFoundException(operationIdentifier, OperationPolicyPointcutParametersFactory.class);
        }
        found = factory;
      }
    }

    PolicyPointcutParameters sourceParameters =
        (PolicyPointcutParameters) ((InternalEvent) event).getInternalParameters().get(POLICY_SOURCE_POINTCUT_PARAMETERS);

    if (found != null) {
      try {
        return found.createPolicyPointcutParameters(operation, operationParameters, sourceParameters);
      } catch (AbstractMethodError error) {
        return found.createPolicyPointcutParameters(operation, operationParameters);
      }
    } else {
      return new PolicyPointcutParameters(operation, sourceParameters);
    }
  }

  private void throwMoreThanOneFactoryFoundException(ComponentIdentifier sourceIdentifier, Class factoryClass) {
    throw new MuleRuntimeException(createStaticMessage(format(
                                                              "More than one %s for component %s was found. There should be only one.",
                                                              factoryClass.getName(), sourceIdentifier)));
  }
}
