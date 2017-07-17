/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry.policy;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.internal.retry.policies.SimpleRetryPolicy;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This policy allows the user to configure how many times a retry should be attempted and how long to wait between retries.
 */
public class SimpleRetryPolicyTemplate extends AbstractPolicyTemplate {

  /**
   * logger used by this class
   */
  protected transient final Logger logger = LoggerFactory.getLogger(SimpleRetryPolicyTemplate.class);

  public static final int DEFAULT_FREQUENCY = 2000;
  public static final int DEFAULT_RETRY_COUNT = 2;
  public static final int RETRY_COUNT_FOREVER = -1;

  protected volatile int count = DEFAULT_RETRY_COUNT;
  protected volatile long frequency = DEFAULT_FREQUENCY;
  private ScheduledExecutorService retryScheduler;

  public SimpleRetryPolicyTemplate() {
    super();
  }

  public SimpleRetryPolicyTemplate(long frequency, int retryCount) {
    // MULE-13092 ExecutionMediator should use scheduler for retry policy
    this(frequency, retryCount, newSingleThreadScheduledExecutor());
  }

  public SimpleRetryPolicyTemplate(long frequency, int retryCount, ScheduledExecutorService retryScheduler) {
    this.frequency = frequency;
    this.count = retryCount;
    this.retryScheduler = retryScheduler;
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

  public RetryPolicy createRetryInstance() {
    return new SimpleRetryPolicy(frequency, count, retryScheduler);
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
