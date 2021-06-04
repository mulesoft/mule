/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.management.execution;

import org.mule.runtime.core.api.event.CoreEvent;

/**
 * Regarding the processing strategy, there are three event that are relevant for profiling:
 *
 * When a @{@link CoreEvent} is dispatched to a scheduler for being processed by a component.
 *
 * When a @{@link CoreEvent} is about to be processed after it has been dispatched.
 *
 * When a response @{@link CoreEvent} is received from the message component before it is dispatched again to the flow.
 *
 * After the response @{@link CoreEvent} has been dispatched to the flow.
 *
 * @since 4.4.0, 4.3.1
 */
public interface ProcessingStrategyExecutionProfiler {

  /**
   * Profiling action before the @{@link CoreEvent} is dispatched to the processor. This may be before a thread switch.
   *
   * @param e the @{@link CoreEvent} to be dispatched.
   */
  void profileBeforeDispatchingToProcessor(CoreEvent e);

  /**
   * Profiling action before the @{@link CoreEvent} is processed by a component after it has been dispatched.
   *
   * @param e the {@link CoreEvent} to be processed.
   */
  void profileBeforeComponentProcessing(CoreEvent e);

  /**
   * Profiling action after @{@link CoreEvent} response is received from the processor.
   *
   * @param e the {@link CoreEvent} is received from the processor.
   */
  void profileAfterResponseReceived(CoreEvent e);

  /**
   * Profiling action after @{@link CoreEvent} is dispatched again to the flow.
   *
   * @param e the {@link CoreEvent} dispatched to the flow.
   */
  void profileAfterDispatchingToFlow(CoreEvent e);
}
