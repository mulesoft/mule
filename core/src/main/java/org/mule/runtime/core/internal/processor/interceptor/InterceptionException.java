/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.processor.interceptor;

/**
 * Default exception thrown when a fail without cause is being called in an interceptionAction.
 *
 * @since 4.0
 */
public class InterceptionException extends RuntimeException {

  public InterceptionException() {}

  public InterceptionException(String message) {
    super(message);
  }

  public InterceptionException(String message, Throwable cause) {
    super(message, cause);
  }

  public InterceptionException(Throwable cause) {
    super(cause);
  }

  public InterceptionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
