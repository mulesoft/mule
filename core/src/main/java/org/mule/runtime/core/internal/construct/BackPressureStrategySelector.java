/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;

import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;

public class BackPressureStrategySelector {

  private static int EVENT_LOOP_SCHEDULER_BUSY_RETRY_INTERVAL_MS = 2;

  private final Logger LOGGER = getLogger(BackPressureStrategySelector.class);

  private AbstractPipeline abstractPipeline;
  private final MessagingExceptionResolver exceptionResolver;

  public BackPressureStrategySelector(AbstractPipeline abstractPipeline) {
    this.abstractPipeline = abstractPipeline;
    this.exceptionResolver = new MessagingExceptionResolver(abstractPipeline);
  }

  protected void checkBackpressureWithWaitStrategy(CoreEvent event) throws FlowBackPressureException {
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
          throw new FlowBackPressureException(abstractPipeline.getName(), ree);
        }
      }
    }
  }

  protected void checkBackpressureWithFailDropStrategy(CoreEvent event) throws FlowBackPressureException {
    try {
      abstractPipeline.getProcessingStrategy().checkBackpressureEmitting(event);
    } catch (RejectedExecutionException ree) {
      throw new FlowBackPressureException(abstractPipeline.getName(), ree);
    }
  }

  public void check(CoreEvent event) throws FlowBackPressureException {
    if (abstractPipeline.getSource().getBackPressureStrategy() == MessageSource.BackPressureStrategy.WAIT) {
      checkBackpressureWithWaitStrategy(event);
    } else {
      checkBackpressureWithFailDropStrategy(event);
    }
  }
}
