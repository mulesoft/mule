/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.concurrent.RejectedExecutionException;

public class BackPressureStrategySelector {

  private static int EVENT_LOOP_SCHEDULER_BUSY_RETRY_INTERVAL_MS = 2;

  private AbstractPipeline abstractPipeline;
  private final MessagingExceptionResolver exceptionResolver;

  public BackPressureStrategySelector(AbstractPipeline abstractPipeline) {
    this.abstractPipeline = abstractPipeline;
    this.exceptionResolver = new MessagingExceptionResolver(abstractPipeline);
  }

  protected void checkBackpressureWithWaitStrategy(CoreEvent event) {
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
          handleOverload(event, new FlowBackPressureException(abstractPipeline.getName(), ree));
        }
      }
    }
  }

  protected void checkBackpressureWithFailDropStrategy(CoreEvent event) {
    try {
      abstractPipeline.getProcessingStrategy().checkBackpressureEmitting(event);
    } catch (RejectedExecutionException ree) {
      handleOverload(event, new FlowBackPressureException(abstractPipeline.getName(), ree));
    }
  }

  private void handleOverload(CoreEvent request, Throwable overloadException) {
    MessagingException me = new MessagingException(request, overloadException, abstractPipeline);
    ((BaseEventContext) request.getContext())
        .error(exceptionResolver.resolve(me, ((PrivilegedMuleContext) abstractPipeline.getMuleContext()).getErrorTypeLocator(),
                                         abstractPipeline.getMuleContext().getExceptionContextProviders()));
  }

  public void check(CoreEvent event) {
    if (abstractPipeline.getSource().getBackPressureStrategy() == MessageSource.BackPressureStrategy.WAIT) {
      checkBackpressureWithWaitStrategy(event);
    } else {
      checkBackpressureWithFailDropStrategy(event);
    }
  }
}
