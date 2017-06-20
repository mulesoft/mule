/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory;

/**
 * Abstract {@link AsyncProcessingStrategyFactory} implementation that supports the configuration of maximum concurrency.
 *
 * @since 4.0
 */
public abstract class AbstractProcessingStrategyFactory implements AsyncProcessingStrategyFactory {

  private int maxConcurrency = DEFAULT_MAX_CONCURRENCY;

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

}
