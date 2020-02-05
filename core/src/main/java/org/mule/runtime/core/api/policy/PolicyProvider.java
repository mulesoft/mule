/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.policy;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.policy.api.PolicyAwareAttribute;
import org.mule.runtime.policy.api.PolicyPointcutParameters;

import java.util.List;
import java.util.Set;

/**
 * Implementation of this interface must provide access to the policies to be applied to message sources or operations.
 *
 * @since 4.0
 */
@NoImplement
public interface PolicyProvider {

  /**
   * Creates a collection of {@link Policy} with the policy chain to be applied to a source.
   * <p>
   * The provided collection must be in the correct order in which the policies must be applied.
   *
   * @param policyPointcutParameters the parameters to use to match against the pointcut configured for each policy.
   * @return the {@link Policy policies} associated to that source.
   */
  List<Policy> findSourceParameterizedPolicies(PolicyPointcutParameters policyPointcutParameters);

  /**
   * @return The attributes that are required by the pointcuts of the currently deployed policies.
   *
   * @since 4.3
   */
  Set<PolicyAwareAttribute> sourcePolicyAwareAtributes();

  /**
   * Creates a collection of {@link Policy} with the policy chain be applied to an operation.
   * <p>
   * The provided collection must be in the correct order in which the policies must be applied.
   *
   * @param policyPointcutParameters the parameters to use to match against the pointcut configured for each policy.
   * @return the {@link Policy policies} associated to that operation.
   */
  List<Policy> findOperationParameterizedPolicies(PolicyPointcutParameters policyPointcutParameters);

  /**
   * Returns whether there are policies registered or not. In case this returns {@code false}, the caller may do certain
   * optimization by skipping altogether the policies code.
   *
   * @since 4.2
   */
  default boolean isPoliciesAvailable() {
    return true;
  };

  /**
   * Register a callback to be executed any time a policy is added or removed.
   *
   * @param policiesChangedCallback
   *
   * @since 4.2
   */
  default void onPoliciesChanged(Runnable policiesChangedCallback) {
    // Nothing to do
  };

}
