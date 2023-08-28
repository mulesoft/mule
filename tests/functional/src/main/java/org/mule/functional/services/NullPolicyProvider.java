/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.services;

import static java.util.Collections.emptyList;
import static org.mule.runtime.http.policy.api.SourcePolicyAwareAttributes.noAttributes;

import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyProvider;
import org.mule.runtime.policy.api.PolicyAwareAttributes;
import org.mule.runtime.policy.api.PolicyPointcutParameters;

import java.util.List;

/**
 * A policy provider that returns no policy.
 *
 * @since 4.0
 */
public class NullPolicyProvider implements PolicyProvider {

  @Override
  public List<Policy> findSourceParameterizedPolicies(PolicyPointcutParameters policyPointcutParameters) {
    return emptyList();
  }

  @Override
  public PolicyAwareAttributes sourcePolicyAwareAttributes() {
    return noAttributes();
  }

  @Override
  public boolean isSourcePoliciesAvailable() {
    return false;
  }

  @Override
  public boolean isOperationPoliciesAvailable() {
    return false;
  }

  @Override
  public List<Policy> findOperationParameterizedPolicies(PolicyPointcutParameters policyPointcutParameters) {
    return emptyList();
  }
}
