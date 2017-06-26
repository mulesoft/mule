/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;

/**
 * Internal {@link RuntimeException} implementation to throw {@link Throwable throwables} that indicates explicitly
 * the {@link ErrorType} that is wanted to throw.
 * Also gives the possibility to declare a message for the {@link Error} being built.
 *
 * @since 4.0
 */
public class TypedException extends MuleRuntimeException {

  private ErrorType errorType;

  /**
   * @param throwable The {@link TypedException#getCause()} of this new exception.
   * @param errorType The {@link ErrorType} that identifies the {@link TypedException#getCause()} {@link Throwable}
   */
  public TypedException(Throwable throwable, ErrorType errorType) {
    super(throwable);
    checkArgument(errorType != null, "The 'errorType' argument can not be null");
    this.errorType = errorType;
  }

  /**
   * @param throwable       The {@link TypedException#getCause()} of this new exception.
   * @param errorType       The {@link ErrorType} that identifies the {@link TypedException#getCause()} {@link Throwable}
   * @param message         error message to override the once from the original exception
   */
  public TypedException(Throwable throwable, ErrorType errorType, String message) {
    super(createStaticMessage(message), throwable);
    checkArgument(errorType != null, "The 'errorType' argument can not be null");
    this.errorType = errorType;
  }

  /**
   * @return The {@link ErrorType} of the thrown exception
   */
  public ErrorType getErrorType() {
    return errorType;
  }
}
