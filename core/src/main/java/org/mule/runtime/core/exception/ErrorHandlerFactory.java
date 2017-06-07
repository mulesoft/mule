/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static java.util.Collections.singletonList;

import org.mule.runtime.core.api.MuleContext;

/**
 * Factory object for {@link ErrorHandler}.
 *
 * @since 4.0
 */
public class ErrorHandlerFactory {

  public ErrorHandler createDefault(MuleContext muleContext) {
    ErrorHandler errorHandler = new ErrorHandler();
    errorHandler.setExceptionListeners(singletonList(new OnErrorPropagateHandler()));
    return errorHandler;
  }
}
