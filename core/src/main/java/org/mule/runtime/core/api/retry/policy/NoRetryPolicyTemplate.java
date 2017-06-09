/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry.policy;

/**
 * This policy is basically a placeholder. It does not attempt to retry at all.
 */
public class NoRetryPolicyTemplate extends AbstractPolicyTemplate {

  public RetryPolicy createRetryInstance() {
    return new NoRetryPolicy();
  }

  protected static class NoRetryPolicy implements RetryPolicy {

    public PolicyStatus applyPolicy(Throwable cause) {
      return PolicyStatus.policyExhausted(cause);
    }
  }

  public String toString() {
    return "NoRetryPolicy{}";
  }
}
