/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor.strategy;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.getInteger;

/**
 *
 * Creates {@link ProcessingStrategy}ies that perform asynchronous processing.
 *
 * @since 4.0
 */
public interface AsyncProcessingStrategyFactory extends ProcessingStrategyFactory {

  int DEFAULT_MAX_CONCURRENCY =
      getInteger(AsyncProcessingStrategyFactory.class.getName() + ".DEFAULT_MAX_CONCURRENCY", MAX_VALUE);

  /**
   * Configures the maximum concurrency permitted. This will typically be used to limit the number of concurrent blocking tasks in
   * execution using the IO pool, but will also limit the number of CPU_LIGHT threads in used concurrently.
   *
   * @param maxConcurrency the maximum concurrency
   */
  void setMaxConcurrency(int maxConcurrency);

}
