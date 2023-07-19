/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.privileged.exception.DefaultExceptionListener;
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

    DefaultExceptionListener exceptionListener = new DefaultExceptionListener();
    exceptionListener.setNotificationFirer(notificationFirer);
    propagate.setExceptionListener(exceptionListener);

    List<MessagingExceptionHandlerAcceptor> exceptionListeners = new ArrayList<>();
    exceptionListeners.add(propagate);
    errorHandler.setExceptionListeners(exceptionListeners);
    return errorHandler;
  }
}
