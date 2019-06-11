/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static java.lang.Thread.interrupted;
import static java.lang.Thread.sleep;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.WAIT;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;

import java.util.concurrent.RejectedExecutionException;

/**
 * Implements the different backpressure handling strategies, and checks against a
 * {@link org.mule.runtime.core.api.processor.strategy.ProcessingStrategy} whether or not backpressure is fired, before and event
 * being processing.
 *
 * @Since 4.3
 */
class BackPressureStrategySelector {

  private static int EVENT_LOOP_SCHEDULER_BUSY_RETRY_INTERVAL_MS = 2;

  private final AbstractPipeline abstractPipeline;
  private final MessagingExceptionResolver exceptionResolver;

  public BackPressureStrategySelector(AbstractPipeline abstractPipeline) {
    this.abstractPipeline = abstractPipeline;
    exceptionResolver = new MessagingExceptionResolver(abstractPipeline);
  }

  /**
   * Wait backpressure strategy. Implements a busy-wait strategy.
   *
   * @param event the event about to begin processing
   * @throws FlowBackPressureException
   */
  protected void checkWithWaitStrategy(CoreEvent event)
      throws FlowBackPressureException {
    boolean accepted = false;
    while (!accepted) {
      try {
        abstractPipeline.getProcessingStrategy().checkBackpressureAccepting(event);
        accepted = true;
      } catch (RejectedExecutionException ree) {
        // TODO MULE-16106 Add a callback for WAIT back pressure applied on the source
        try {
          sleep(EVENT_LOOP_SCHEDULER_BUSY_RETRY_INTERVAL_MS);
        } catch (InterruptedException e) {
          // Clear interrupt flag on thread
          interrupted();
          throw new FlowBackPressureException(abstractPipeline.getName(), ree);
        }
      }
    }
  }

  /**
   * Drop backpressure strategy. If backpressure is fired on the incoming event, it gets dropped from processing.
   *
   * @param event the event about to begin processing
   * @throws FlowBackPressureException
   */
  protected void checkWithFailDropStrategy(CoreEvent event)
      throws FlowBackPressureException {
    if (!abstractPipeline.getProcessingStrategy().checkBackpressureEmitting(event)) {
      throw new FlowBackPressureException(abstractPipeline.getName());
    }
  }

  /**
   * Decides which {@link org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy} to apply for a certain
   * {@link MessageSource}, and check whether a backpressure signal will be fired upon entering the processing stage.
   *
   * @param event the event about to begin processing
   * @throws FlowBackPressureException
   */
  public void check(CoreEvent event)
      throws FlowBackPressureException {
    if (abstractPipeline.getSource().getBackPressureStrategy() == WAIT) {
      checkWithWaitStrategy(event);
    } else {
      checkWithFailDropStrategy(event);
    }
  }
}
