/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.privileged.exception.MessagingExceptionHandlerAcceptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory object for {@link ErrorHandler}.
 *
 * @since 4.0
 */
public class ErrorHandlerFactory {

  public ErrorHandler createDefault(NotificationDispatcher notificationFirer) {
    ErrorHandler errorHandler = new ErrorHandler();
    final OnErrorPropagateHandler propagate = new OnErrorPropagateHandler();
    propagate.setNotificationFirer(notificationFirer);
    List<MessagingExceptionHandlerAcceptor> exceptionListeners = new ArrayList<>();
    exceptionListeners.add(propagate);
    errorHandler.setExceptionListeners(exceptionListeners);
    return errorHandler;
  }
}
