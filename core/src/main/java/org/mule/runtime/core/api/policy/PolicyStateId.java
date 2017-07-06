/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.policy;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

/**
 * Identifier for a policy state.
 * 
 * A policy state identifier is the composition of the execution identifier which is unique accross every execution and the policy
 * id which is unique across all available policies.
 * 
 * @since 4.0
 */
public class PolicyStateId {

  private String executionIdentifier;
  private String policyId;

  /**
   * Creates a new policy state id.
   *
   * @param executionIdentifier identifier of the execution of the policy
   * @param policyId identifier of the policy
   */
  public PolicyStateId(String executionIdentifier, String policyId) {
    checkArgument(!isEmpty(executionIdentifier), "executionIdentifier cannot be null or empty");
    checkArgument(!isEmpty(executionIdentifier), "policyId cannot be null or empty");
    this.executionIdentifier = executionIdentifier;
    this.policyId = policyId;
  }

  /**
   * @return the identifier of the execution
   */
  public String getExecutionIdentifier() {
    return executionIdentifier;
  }

  /**
   * @return the identifier of the policy
   */
  public String getPolicyId() {
    return policyId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PolicyStateId that = (PolicyStateId) o;

    if (executionIdentifier != null ? !executionIdentifier.equals(that.executionIdentifier)
        : that.executionIdentifier != null) {
      return false;
    }
    return policyId != null ? policyId.equals(that.policyId) : that.policyId == null;

  }

  @Override
  public int hashCode() {
    int result = executionIdentifier != null ? executionIdentifier.hashCode() : 0;
    result = 31 * result + (policyId != null ? policyId.hashCode() : 0);
    return result;
  }
}
