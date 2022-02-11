/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import org.mule.runtime.core.internal.exception.ErrorHandler;
import org.mule.runtime.core.internal.exception.GlobalErrorHandler;
import org.mule.runtime.core.privileged.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An {@link org.mule.runtime.dsl.api.component.ObjectFactory} which produces {@link ErrorHandler} instances.
 *
 * @since 4.1.0
 */
public class ErrorHandlerFactoryBean extends AbstractComponentFactory<ErrorHandler> {

  private static Map<String, ErrorHandler> globalErrorHandlers = new HashMap<String, ErrorHandler>();
  private GlobalErrorHandler delegate;
  private List<MessagingExceptionHandlerAcceptor> exceptionListeners;
  private String name;

  @Override
  public ErrorHandler doGetObject() throws Exception {
    if (delegate != null) {
      return delegate;
    }

    ErrorHandler errorHandler;
    if (isGlobalErrorHandler()) {
      if (globalErrorHandlers.containsKey(name)) {
        return globalErrorHandlers.get(name);
      } else {
        errorHandler = new GlobalErrorHandler();
        errorHandler.setName(name);
        globalErrorHandlers.put(name, errorHandler);
      }
    } else {
      errorHandler = new ErrorHandler();
    }
    errorHandler.setExceptionListeners(exceptionListeners);
    return errorHandler;
  }

  private boolean isGlobalErrorHandler() {
    return getLocation().getParts().size() == 1;
  }

  public void setDelegate(GlobalErrorHandler delegate) {
    this.delegate = delegate;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setExceptionListeners(List<MessagingExceptionHandlerAcceptor> exceptionListeners) {
    this.exceptionListeners = exceptionListeners;
  }

}
