/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mule.runtime.core.DefaultMuleEvent.getCurrentEvent;
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;

import org.mule.runtime.core.api.ExceptionPayload;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.exception.RollbackSourceCallback;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.connector.ConnectException;
import org.mule.runtime.core.message.DefaultExceptionPayload;
import org.mule.runtime.core.transaction.TransactionCoordination;

/**
 * Fire a notification, log exception, clean up transaction if any, and trigger reconnection strategy if this is a
 * <code>ConnectException</code>.
 */
public abstract class AbstractSystemExceptionStrategy extends AbstractExceptionListener implements SystemExceptionHandler {

  @Override
  public void handleException(Exception ex, RollbackSourceCallback rollbackMethod) {
    fireNotification(ex);

    doLogException(ex);

    if (isRollback(ex)) {
      logger.debug("Rolling back transaction");
      rollback(ex, rollbackMethod);
    } else {
      logger.debug("Committing transaction");
      commit();
    }

    ExceptionPayload exceptionPayload = new DefaultExceptionPayload(ex);
    if (getCurrentEvent() != null) {
      MuleEvent currentEvent = getCurrentEvent();
      currentEvent.setMessage(MuleMessage.builder(currentEvent.getMessage()).exceptionPayload(exceptionPayload).build());
      setCurrentEvent(currentEvent);
    }

    if (ex instanceof ConnectException) {
      ((ConnectException) ex).handleReconnection();
    }
  }

  private void rollback(Exception ex, RollbackSourceCallback rollbackMethod) {
    if (TransactionCoordination.getInstance().getTransaction() != null) {
      rollback(ex);
    }
    if (rollbackMethod != null) {
      rollbackMethod.rollback();
    }
  }

  @Override
  public void handleException(Exception ex) {
    handleException(ex, null);
  }
}


