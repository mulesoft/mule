/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import org.mule.sdk.api.runtime.exception.ExceptionHandler;

/**
 * Adapts a legacy {@link org.mule.runtime.extension.api.runtime.exception.ExceptionHandler} into an sdk-api
 * {@link ExceptionHandler}
 *
 * @since 4.5.0
 */
public class SdkExceptionHandlerAdapter extends ExceptionHandler {

  private final org.mule.runtime.extension.api.runtime.exception.ExceptionHandler delegate;

  public static ExceptionHandler from(Object value) {
    if (value instanceof ExceptionHandler) {
      return (ExceptionHandler) value;
    } else if (value instanceof org.mule.runtime.extension.api.runtime.exception.ExceptionHandler) {
      return new SdkExceptionHandlerAdapter((org.mule.runtime.extension.api.runtime.exception.ExceptionHandler) value);
    } else {
      throw new IllegalArgumentException("Unsupported type: " + value.getClass());
    }
  }

  public SdkExceptionHandlerAdapter(org.mule.runtime.extension.api.runtime.exception.ExceptionHandler exceptionHandler) {
    this.delegate = exceptionHandler;
  }

  @Override
  public Exception enrichException(Exception e) {
    return delegate.enrichException(e);
  }
}
