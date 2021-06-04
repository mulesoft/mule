/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.management.provider;

import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.management.execution.ExecutionOrchestrator;
import org.mule.runtime.core.internal.management.execution.ExecutionStrategy;
import org.mule.runtime.core.internal.management.execution.ProcessingStrategyExecutionProfiler;
import org.mule.runtime.core.internal.management.pipeline.ProcessingStrategyPipelineProfiler;

/**
 * Provider of management utils.
 *
 * @since 4.4.0, 4.3.1
 */
public interface MuleManagementUtilsProvider {

  /**
   * Gets an {@link ProcessingStrategyExecutionProfiler} for a location
   *
   * @param processor processor
   * @return execution interceptor
   */
  ProcessingStrategyExecutionProfiler getProcessingStrategyExecutionProfiler(ReactiveProcessor processor);

  /**
   * Gets an {@link ExecutionOrchestrator} for a location
   *
   * @param processor processor
   * @return execution tracer
   */
  ExecutionOrchestrator getExecutionOrchestrator(ReactiveProcessor processor, ExecutionStrategy executionStrategy);

  /**
   * Gets a @{link ProcessingStrategyPipelineProfiler} for a pipele
   * 
   * @return the pipeline profiler
   */
  ProcessingStrategyPipelineProfiler getProcessingStrategyPipelineProfiler(ReactiveProcessor pipeline);
}
