/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry.policy;

import org.mule.runtime.core.internal.retry.policies.SimpleRetryPolicy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This policy allows the user to configure how many times a retry should be attempted and how long to wait between retries.
 */
public final class SimpleRetryPolicyTemplate extends AbstractPolicyTemplate {

  /**
   * logger used by this class
   */
  protected transient final Logger logger = LoggerFactory.getLogger(SimpleRetryPolicyTemplate.class);

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
