/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.source.MessageSource;

import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;

public class BackPressureStrategySelector {

  private static int EVENT_LOOP_SCHEDULER_BUSY_RETRY_INTERVAL_MS = 2;

  private final Logger LOGGER = getLogger(BackPressureStrategySelector.class);

  private AbstractPipeline abstractPipeline;

  public BackPressureStrategySelector(AbstractPipeline abstractPipeline) {
    this.abstractPipeline = abstractPipeline;
  }

  protected void checkBackpressureWithWaitStrategy(CoreEvent event, FlowExceptionHandler exceptionHandler)
      throws FlowBackPressureException {
    boolean accepted = false;
    while (!accepted) {
      try {
        abstractPipeline.getProcessingStrategy().checkBackpressureAccepting(event);
        accepted = true;
      } catch (RejectedExecutionException ree) {
        // TODO MULE-16106 Add a callback for WAIT back pressure applied on the source
        try {
          Thread.sleep(EVENT_LOOP_SCHEDULER_BUSY_RETRY_INTERVAL_MS);
        } catch (InterruptedException e) {
          FlowBackPressureException exception = new FlowBackPressureException(abstractPipeline.getName(), ree);
          exceptionHandler.handleException(exception, event);
          throw exception;
        }
      }
    }
  }

  protected void checkBackpressureWithFailDropStrategy(CoreEvent event, FlowExceptionHandler exceptionHandler)
      throws FlowBackPressureException {
    if (!abstractPipeline.getProcessingStrategy().checkBackpressureEmitting(event)) {
      FlowBackPressureException exception = new FlowBackPressureException(abstractPipeline.getName());
      exceptionHandler.handleException(exception, event);
      throw exception;
    }
  }

  public void checkNotifyingExceptionListener(CoreEvent event, FlowExceptionHandler exceptionHandler)
      throws FlowBackPressureException {
    if (abstractPipeline.getSource().getBackPressureStrategy() == MessageSource.BackPressureStrategy.WAIT) {
      checkBackpressureWithWaitStrategy(event, exceptionHandler);
    } else {
      checkBackpressureWithFailDropStrategy(event, exceptionHandler);
    }
  }
}
