/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import static org.mule.runtime.core.util.Preconditions.checkState;

import org.mule.runtime.api.message.ErrorType;

/**
 * Mapping between a {@link Exception} type and an {@link ErrorType}.
 * 
 * @since 4.0
 */
public class ExceptionMapping implements Comparable<ExceptionMapping> {

  private Class<? extends Exception> exceptionType;
  private ErrorType errorType;

  /**
   * Creates a new {@link ExceptionMapper} instance.
   * 
   * @param exceptionType exception type to related to the error type.
   * @param errorType error type associated with the exception type.
   */
  ExceptionMapping(Class<? extends Exception> exceptionType, ErrorType errorType) {
    checkState(exceptionType != null, "exceptionType type cannot be null");
    checkState(errorType != null, "error type cannot be null");
    this.exceptionType = exceptionType;
    this.errorType = errorType;
  }

  /**
   * @param exception exception to check if it matches with this mapping.
   * @return true if the exception type is associated with this mapping.
   */
  public boolean matches(Exception exception) {
    return this.exceptionType.isAssignableFrom(exception.getClass());
  }

  /**
   * @return the error type of this mapping.
     */
  public ErrorType getErrorType() {
    return errorType;
  }

  @Override
  public int compareTo(ExceptionMapping exceptionMapping) {
    if (this.exceptionType.isAssignableFrom(exceptionMapping.exceptionType)) {
      return 1;
    }
    if (exceptionMapping.exceptionType.isAssignableFrom(this.exceptionType)) {
      return -1;
    }
    if (exceptionType.equals(exceptionMapping.exceptionType)) {
      return 0;
    }
    return 1;
  }
}
