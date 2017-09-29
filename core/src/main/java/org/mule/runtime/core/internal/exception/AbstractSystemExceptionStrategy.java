/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.getCurrentEvent;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.setCurrentEvent;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ConnectException;
import org.mule.runtime.core.api.exception.RollbackSourceCallback;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.api.message.ExceptionPayload;
import org.mule.runtime.core.internal.message.DefaultExceptionPayload;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.exception.AbstractExceptionListener;

/**
 * Fire a notification, log exception, clean up transaction if any, and trigger reconnection strategy if this is a
 * <code>ConnectException</code>.
 */
public abstract class AbstractSystemExceptionStrategy extends AbstractExceptionListener implements SystemExceptionHandler {

  protected Scheduler retryScheduler;

  @Override
  public void handleException(Exception ex, RollbackSourceCallback rollbackMethod) {
    fireNotification(ex, getCurrentEvent());

    doLogException(ex);

    logger.debug("Rolling back transaction");
    rollback(ex, rollbackMethod);

    ExceptionPayload exceptionPayload = new DefaultExceptionPayload(ex);
    if (getCurrentEvent() != null) {
      PrivilegedEvent currentEvent = getCurrentEvent();
      currentEvent = PrivilegedEvent.builder(currentEvent)
          .message(InternalMessage.builder(currentEvent.getMessage()).exceptionPayload(exceptionPayload).build()).build();
      setCurrentEvent(currentEvent);
    }

    if (ex instanceof ConnectException) {
      ((ConnectException) ex).handleReconnection(retryScheduler);
    }
  }

  private void rollback(Exception ex, RollbackSourceCallback rollbackMethod) {
    rollback(ex);
    if (rollbackMethod != null) {
      rollbackMethod.rollback();
    }
  }

  @Override
  public void handleException(Exception ex) {
    handleException(ex, null);
  }

  @Override
  protected void doInitialise(MuleContext context) throws InitialisationException {
    retryScheduler =
        muleContext.getSchedulerService().ioScheduler(muleContext.getSchedulerBaseConfig().withShutdownTimeout(0, MILLISECONDS));
    super.doInitialise(context);
  }

  @Override
  public void dispose() {
    super.dispose();
    if (retryScheduler != null) {
      retryScheduler.stop();
      retryScheduler = null;
    }
  }
}
