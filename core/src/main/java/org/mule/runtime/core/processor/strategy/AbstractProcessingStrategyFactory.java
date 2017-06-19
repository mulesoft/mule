/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

/**
 * Creates ring-buffer based processing strategy instances. These processing strategy de-multiplex incoming messages using a
 * ring-buffer which can then be subscribed to n times.
 * <p>
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public abstract class AbstractProcessingStrategyFactory implements ProcessingStrategyFactory {

  private static int DEFAULT_MAX_CONCURRENCY = Integer.MAX_VALUE;
  private int maxConcurrency = DEFAULT_MAX_CONCURRENCY;

  /**
   * Configures the maximum concurrency permitted. This will typically be used to limit the number of concurrent blocking tasks
   * using the IO pool, but will also limit the number of CPU_LIGHT threads in used concurrently.
   *
   * @param maxConcurrency the maximum concurrency
   */
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
  public int getMaxConcurrency() {
    return maxConcurrency;
  }

}
