/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;

import java.util.Optional;

/**
 * Default implementation for {@link OperationPolicyFactory}.
 *
 * @since 4.0
 */
public class DefaultOperationPolicyFactory implements OperationPolicyFactory {

  private final PolicyStateHandler policyStateHandler;

  public DefaultOperationPolicyFactory(PolicyStateHandler policyStateHandler) {
    this.policyStateHandler = policyStateHandler;
  }

  @Override
  public OperationPolicy createOperationPolicy(Policy policy,
                                               Optional<OperationPolicyParametersTransformer> operationPolicyParametersTransformer) {
    return new DefaultOperationPolicy(policy, operationPolicyParametersTransformer, policyStateHandler);
  }
}
