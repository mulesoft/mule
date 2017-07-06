/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.policy;

import org.mule.runtime.policy.api.PolicyPointcutParameters;

import java.util.List;

/**
 * Implementation of this interface must provide access to the policies to be applied to message sources or operations.
 *
 * @since 4.0
 */
public interface PolicyProvider {

  /**
   * Creates a collection of {@link Policy} with the policy chain to be applied to a source.
   * <p>
   * The provided collection must be in the correct order in which the policies must be applied.
   *
   * @param policyPointcutParameters the parameters to use to match against the pointcut configured for each policy.
   * @return a {@link OperationPolicyProcessor} associated to that source.
   */
  List<Policy> findSourceParameterizedPolicies(PolicyPointcutParameters policyPointcutParameters);

  /**
   * Creates a collection of {@link Policy} with the policy chain be applied to an operation.
   * <p>
   * The provided collection must be in the correct order in which the policies must be applied.
   *
   * @param policyPointcutParameters the parameters to use to match against the pointcut configured for each policy.
   * @return a {@link OperationPolicyProcessor} associated to that source.
   */
  List<Policy> findOperationParameterizedPolicies(PolicyPointcutParameters policyPointcutParameters);

}
