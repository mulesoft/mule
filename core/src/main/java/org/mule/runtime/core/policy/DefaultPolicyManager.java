/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
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
    Optional<PolicyChain> sourcePolicyChainOptional = policyProvider.findSourcePolicyChain(sourceIdentifier);
    return sourcePolicyChainOptional.map(sourcePolicyChain -> {
      Optional<SourcePolicyParametersTransformer> sourcePolicyParametersTransformer =
          lookupSourceParametersTransformer(sourceIdentifier);
      return Optional
          .of(new SourcePolicy(sourcePolicyChainOptional.get(), sourcePolicyParametersTransformer, policyStateHandler));
    }).orElse(empty());
  }

  @Override
  public Optional<OperationPolicy> findOperationPolicy(String executionIdentifier, ComponentIdentifier operationIdentifier) {
    Optional<PolicyChain> operationPolicyChainOptional = policyProvider.findOperationPolicyChain(operationIdentifier);
    return operationPolicyChainOptional.map(operationPolicyChain -> {
      Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer =
          lookupOperationParametersTransformer(operationIdentifier);
      return Optional
          .of(new OperationPolicy(operationPolicyChainOptional.get(), operationPolicyParametersTransformer,
                                  policyStateHandler));
    }).orElse(empty());
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
