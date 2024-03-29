/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

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
