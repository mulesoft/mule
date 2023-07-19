/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory;

/**
 * Abstract {@link AsyncProcessingStrategyFactory} implementation that supports the configuration of maximum concurrency.
 *
 * @since 4.0
 */
public abstract class AbstractProcessingStrategyFactory implements AsyncProcessingStrategyFactory {

  private int maxConcurrency = DEFAULT_MAX_CONCURRENCY;
  private boolean maxConcurrencyEagerCheck = true;

  /**
   * Configures the maximum concurrency permitted. This will typically be used to limit the number of concurrent blocking tasks
   * using the IO pool, but will also limit the number of CPU_LIGHT threads in used concurrently.
   *
   * @param maxConcurrency the maximum concurrency
   */
  @Override
  public void setMaxConcurrency(int maxConcurrency) {
    if (maxConcurrency < 1) {
      throw new IllegalArgumentException("maxConcurrency must be at least 1");
    }
    this.maxConcurrency = maxConcurrency;
  }

  /**
   * The maximum concurrency permitted. This will typically be used to limit the number of concurrent blocking tasks using the IO
   * pool, but will also limit the number of CPU_LIGHT threads in used concurrently.
   *
   * @return the maximum concurrency
   */
  protected int getMaxConcurrency() {
    return maxConcurrency;
  }

  @Override
  public void setMaxConcurrencyEagerCheck(boolean maxConcurrencyEagerCheck) {
    this.maxConcurrencyEagerCheck = maxConcurrencyEagerCheck;
  }

  public boolean isMaxConcurrencyEagerCheck() {
    return maxConcurrencyEagerCheck;
  }
}
