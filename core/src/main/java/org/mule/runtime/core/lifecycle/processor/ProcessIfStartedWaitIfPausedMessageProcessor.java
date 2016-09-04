/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.lifecycle.processor;

import static org.mule.runtime.core.config.i18n.CoreMessages.interruptedWaitingForPaused;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleEvent.Builder;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.service.Pausable;

public class ProcessIfStartedWaitIfPausedMessageProcessor extends ProcessIfStartedMessageProcessor {

  public ProcessIfStartedWaitIfPausedMessageProcessor(Startable startable, LifecycleState lifecycleState) {
    super(startable, lifecycleState);
  }

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    Builder builder = MuleEvent.builder(event);
    if (accept(event, builder)) {
      if (isPaused()) {
        try {
          if (logger.isDebugEnabled()) {
            logger.debug(startable.getClass().getName() + " " + getStartableName(startable)
                + " is paused. Blocking call until resumed");
          }
          while (isPaused()) {
            Thread.sleep(500);
          }
        } catch (InterruptedException e) {
          throw new MessagingException(interruptedWaitingForPaused(getStartableName(startable)), event, e, this);
        }
      }
      return processNext(builder.build());
    } else {
      return handleUnaccepted(builder.build());
    }
  }

  @Override
  protected boolean accept(MuleEvent event, MuleEvent.Builder builder) {
    return lifecycleState.isStarted() || isPaused();
  }

  protected boolean isPaused() {
    return lifecycleState.isPhaseComplete(Pausable.PHASE_NAME);
  }

}
