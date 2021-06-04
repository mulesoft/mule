/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.provider;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.management.execution.LoggerProcessingStrategyExecutionProfiler;
import org.mule.runtime.core.internal.management.execution.ExecutionOrchestrator;
import org.mule.runtime.core.internal.management.execution.ExecutionStrategy;
import org.mule.runtime.core.internal.management.execution.ProcessingStrategyExecutionProfiler;
import org.mule.runtime.core.internal.management.execution.StreamExecutionOrchestrator;
import org.mule.runtime.core.internal.management.pipeline.LoggerProcessingStrategyPipelineProfiler;
import org.mule.runtime.core.internal.management.pipeline.ProcessingStrategyPipelineProfiler;
import org.mule.runtime.core.internal.processor.chain.InterceptedReactiveProcessor;
import org.mule.runtime.core.internal.processor.strategy.ComponentInnerProcessor;

/**
 * Default implementation fo a @{@link MuleManagementUtilsProvider}
 *
 * @since 4.4.0, 4.3.1
 */
public class DefaultMuleManagementUtilsProvider implements MuleManagementUtilsProvider {

  @Override
  public ProcessingStrategyExecutionProfiler getProcessingStrategyExecutionProfiler(ReactiveProcessor processor) {
    return new LoggerProcessingStrategyExecutionProfiler(getLocation(processor));
  }

  @Override
  public ExecutionOrchestrator getExecutionOrchestrator(ReactiveProcessor processor, ExecutionStrategy executionStrategy) {
    return new StreamExecutionOrchestrator(executionStrategy);
  }

  @Override
  public ProcessingStrategyPipelineProfiler getProcessingStrategyPipelineProfiler(ReactiveProcessor pipeline) {
    return new LoggerProcessingStrategyPipelineProfiler(getLocation(pipeline));
  }

  private ComponentLocation getLocation(ReactiveProcessor processor) {
    if (processor instanceof ComponentInnerProcessor) {
      return ((ComponentInnerProcessor) processor).getLocation();
    }

    if (processor instanceof InterceptedReactiveProcessor) {
      return getLocation(((InterceptedReactiveProcessor) processor).getProcessor());
    }

    if (processor instanceof Component) {
      return ((Component) processor).getLocation();
    }

    return null;
  }

}
