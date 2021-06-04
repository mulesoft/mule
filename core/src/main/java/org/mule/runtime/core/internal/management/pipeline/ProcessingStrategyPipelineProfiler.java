/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.management.pipeline;

import org.mule.runtime.core.api.event.CoreEvent;

/**
 * Profiler for tracking the events that are dispatched to a pipeline through a processing strategy.
 * 
 * @since 4.4.0, 4.3.1
 */
public interface ProcessingStrategyPipelineProfiler {

  /**
   * Profiling action before the @{@link CoreEvent} is dispatched to the flow. A thread switch may be present.
   * 
   * @param e the {@link CoreEvent} that will be dispatched to the flow.
   */
  void profileBeforeDispatchingToPipeline(CoreEvent e);

  /**
   * Profiling action after the @{@link CoreEvent} was processed by
   *
   * @param e the {@link CoreEvent} that finished processing
   */
  void profileAfterPipelineProcessed(CoreEvent e);
}
