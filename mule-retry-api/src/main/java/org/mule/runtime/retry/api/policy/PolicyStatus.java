/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.retry.api.policy;


import org.mule.runtime.retry.internal.DefaultPolicyStatus;

/**
 * Indicates the current state of a RetryPolicy
 * <ul>
 * <li>ok: The policy is active</li>
 * <li>exhausted: The policy has run through the actions for the policy</li>
 * </ul>
 *
 * For example, a RetryPolicy may have a RetryCount - how many times the policy can be invoked. Once the retryCount has been
 * reached, the policy is exhausted and cannot be used again.
 */
public interface PolicyStatus {

  static PolicyStatus policyExhausted(Throwable t) {
    return new DefaultPolicyStatus(true, t);
  }

  static PolicyStatus policyOk() {
    return new DefaultPolicyStatus();
  }

  boolean isExhausted();

  boolean isOk();

  Throwable getThrowable();

}
