/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.policy.PolicyOperationParametersTransformer;
import org.mule.runtime.core.api.policy.PolicySourceParametersTransformer;
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

  private Collection<PolicyOperationParametersTransformer> policyOperationParametersTransformerCollection = emptyList();
  private Collection<PolicySourceParametersTransformer> policySourceParametersTransformerCollection = emptyList();
  private PolicyProvider policyProvider;

  @Override
  public PolicyProvider lookupPolicyProvider() {
    return policyProvider;
  }

  @Override
  public Optional<PolicyOperationParametersTransformer> lookupOperationParametersTransformer(ComponentIdentifier componentIdentifier) {
    return policyOperationParametersTransformerCollection.stream()
        .filter(policyOperationParametersTransformer -> policyOperationParametersTransformer.supports(componentIdentifier))
        .findAny();
  }

  @Override
  public Optional<PolicySourceParametersTransformer> lookupSourceParametersTransformer(ComponentIdentifier componentIdentifier) {
    return policySourceParametersTransformerCollection.stream()
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
      policySourceParametersTransformerCollection =
          muleContext.getRegistry().lookupObjects(PolicySourceParametersTransformer.class);
      policyOperationParametersTransformerCollection =
          muleContext.getRegistry().lookupObjects(PolicyOperationParametersTransformer.class);
    } catch (RegistrationException e) {
      throw new InitialisationException(e, this);
    }
  }
}
