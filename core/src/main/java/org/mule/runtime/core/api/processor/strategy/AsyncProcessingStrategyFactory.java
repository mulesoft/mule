/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.processor.strategy;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.getInteger;

import org.mule.api.annotation.NoImplement;

/**
 *
 * Creates {@link ProcessingStrategy}ies that perform asynchronous processing.
 *
 * @since 4.0
 */
@NoImplement
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

  /**
   * Configures whether {@code maxConcurrency} should be enforced when dispatching an event to the pipeline by rejecting it.
   * <p>
   * If set to {@code false}, the event will be received but it may be queued or otherwise delayed due to {@code maxConcurrency},
   * but it will not be rejected.
   *
   * @param maxConcurrencyEagerCheck
   */
  void setMaxConcurrencyEagerCheck(boolean maxConcurrencyEagerCheck);
}
