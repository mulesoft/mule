/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
