/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.retry.policy;

import org.mule.runtime.core.internal.retry.policies.SimpleRetryPolicy;

/**
 * This policy allows the user to configure how many times a retry should be attempted and how long to wait between retries.
 */
public class SimpleRetryPolicyTemplate extends AbstractPolicyTemplate {

  public static final int DEFAULT_FREQUENCY = 2000;
  public static final int DEFAULT_RETRY_COUNT = 2;
  public static final int RETRY_COUNT_FOREVER = -1;

  protected volatile int count = DEFAULT_RETRY_COUNT;
  protected volatile long frequency = DEFAULT_FREQUENCY;

  public SimpleRetryPolicyTemplate() {
    super();
  }

  public SimpleRetryPolicyTemplate(long frequency, int retryCount) {
    this.frequency = frequency;
    this.count = retryCount;
  }

  public long getFrequency() {
    return frequency;
  }

  public int getCount() {
    return count;
  }

  public void setFrequency(long frequency) {
    this.frequency = frequency;
  }

  public void setCount(int count) {
    this.count = count;
  }

  @Override
  public RetryPolicy createRetryInstance() {
    return new SimpleRetryPolicy(frequency, count);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("SimpleRetryPolicy");
    sb.append("{frequency=").append(frequency);
    sb.append(", retryCount=").append(count);
    sb.append('}');

    return sb.toString();
  }
}
