/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.policy;

import org.mule.api.annotation.NoExtend;

/**
 * This class represents a policy injection.
 * <p>
 * It contains the set of parameters configured to apply the policy and the chain of
 * {@link org.mule.runtime.core.api.processor.Processor}s to be applied.
 */
@NoExtend
public class Policy {

  private final PolicyChain policyChain;
  private final String id;

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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Policy other = (Policy) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

}
