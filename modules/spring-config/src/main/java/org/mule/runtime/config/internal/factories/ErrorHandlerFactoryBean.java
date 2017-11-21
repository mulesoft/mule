/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import org.mule.runtime.core.internal.exception.ErrorHandler;
import org.mule.runtime.core.privileged.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import java.util.List;

/**
 * An {@link org.mule.runtime.dsl.api.component.ObjectFactory} which produces {@link ErrorHandler} instances.
 *
 * @since 4.1.0
 */
public class ErrorHandlerFactoryBean extends AbstractComponentFactory<ErrorHandler> {

  private ErrorHandler delegate;
  private List<MessagingExceptionHandlerAcceptor> exceptionListeners;
  private String name;

  @Override
  public ErrorHandler doGetObject() throws Exception {
    ErrorHandler errorHandler;
    if (delegate != null) {
      errorHandler = delegate;
    } else {
      errorHandler = new ErrorHandler();
      errorHandler.setName(name);
      errorHandler.setExceptionListeners(exceptionListeners);
    }
    return errorHandler;
  }

  public void setDelegate(ErrorHandler delegate) {
    this.delegate = delegate;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setExceptionListeners(List<MessagingExceptionHandlerAcceptor> exceptionListeners) {
    this.exceptionListeners = exceptionListeners;
  }

}
