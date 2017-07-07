/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.policy;

import org.mule.runtime.core.api.policy.PolicyChain;

/**
 * This class represents a policy injection.
 * <p>
 * It contains the set of parameters configured to apply the policy and the chain of
 * {@link org.mule.runtime.core.api.processor.Processor}s to be applied.
 */
public class Policy {

  private final PolicyChain policyChain;
  private String id;

  /**
   * Creates a new {@code ParameterizedPolicy}.
   * 
   * @param policyChain the chain of {@link org.mule.runtime.core.api.processor.Processor}s to be applied.
   * @param policyId unique id of this policy.
   */
  public Policy(PolicyChain policyChain, String policyId) {
    this.policyChain = policyChain;
    this.id = policyId;
  }

  /**
   * @return the unique id for this policy.
   */
  public String getPolicyId() {
    return id;
  }

  /**
   * Retrieves the chain with the logic related to the policy.
   *
   * @return a chain of {@link org.mule.runtime.core.api.processor.Processor}s to be applied.
   */
  public PolicyChain getPolicyChain() {
    return policyChain;
  }

}
