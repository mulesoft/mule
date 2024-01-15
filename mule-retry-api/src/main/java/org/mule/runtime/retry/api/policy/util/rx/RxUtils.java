/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.retry.api.policy.util.rx;

public class RxUtils {

  private static final String REACTIVE_EXCEPTION_CLASS_NAME = "reactor.core.Exceptions$ReactiveException";

  /**
   * Unwrap a particular {@code Throwable}.
   *
   * @param throwable the exception to wrap
   * @return the unwrapped exception
   */
  public static Throwable unwrap(Throwable throwable) {
    while (throwable.getClass().getName().equals(REACTIVE_EXCEPTION_CLASS_NAME)) {
      throwable = throwable.getCause();
    }
    return throwable;
  }

}
