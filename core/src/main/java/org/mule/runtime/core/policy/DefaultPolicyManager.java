/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import static java.util.Collections.emptyList;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.Collection;
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

  private Collection<OperationPolicyParametersTransformer> operationPolicyParametersTransformerCollection = emptyList();
  private Collection<SourcePolicyParametersTransformer> sourcePolicyParametersTransformerCollection = emptyList();
  private PolicyProvider policyProvider;

  @Override
  public Optional<SourcePolicy> findSourcePolicyInstance(String executionIdentifier, ComponentIdentifier sourceIdentifier) {
    Optional<AbstractPolicyChain> sourcePolicyInstance = policyProvider.findSourcePolicyInstance(sourceIdentifier);
    if (sourcePolicyInstance.isPresent()) {
      Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer =
          lookupSourceParametersTransformer(sourceIdentifier);
      return Optional
          .of(new DefaultSourcePolicy(sourcePolicyInstance.get(), sourcePolicyParametersTransformer, policyStateHandler));
    }
    return Optional.empty();
  }

  @Override
  public Optional<OperationPolicy> findOperationPolicy(String executionIdentifier, ComponentIdentifier operationIdentifier) {
    Optional<AbstractPolicyChain> sourcePolicyInstance = policyProvider.findSourcePolicyInstance(operationIdentifier);
    if (sourcePolicyInstance.isPresent()) {
      Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer =
          lookupOperationParametersTransformer(operationIdentifier);
      return Optional
          .of(new DefaultOperationPolicy(sourcePolicyInstance.get(), operationPolicyParametersTransformer, policyStateHandler));
    }
    return Optional.empty();
  }

  @Override
  public Optional<OperationPolicyParametersTransformer> lookupOperationParametersTransformer(ComponentIdentifier componentIdentifier) {
    return operationPolicyParametersTransformerCollection.stream()
        .filter(policyOperationParametersTransformer -> policyOperationParametersTransformer.supports(componentIdentifier))
        .findAny();
  }

  @Override
  public Optional<SourcePolicyParametersTransformer> lookupSourceParametersTransformer(ComponentIdentifier componentIdentifier) {
    return sourcePolicyParametersTransformerCollection.stream()
        .filter(policyOperationParametersTransformer -> policyOperationParametersTransformer.supports(componentIdentifier))
        .findAny();
  }


  @Override
  public void initialise() throws InitialisationException {
    try {
      policyProvider = muleContext.getRegistry().lookupObject(PolicyProvider.class);
      if (policyProvider == null) {
        policyProvider = new NullPolicyProvider();
      }
      sourcePolicyParametersTransformerCollection =
          muleContext.getRegistry().lookupObjects(SourcePolicyParametersTransformer.class);
      operationPolicyParametersTransformerCollection =
          muleContext.getRegistry().lookupObjects(OperationPolicyParametersTransformer.class);
    } catch (RegistrationException e) {
      throw new InitialisationException(e, this);
    }
  }

  @Override
  public void disposePoliciesResources(String executionIdentifier) {
    policyStateHandler.destroyState(executionIdentifier);
  }
}
