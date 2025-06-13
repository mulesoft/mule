/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.span.command;

import java.util.function.Function;

import org.slf4j.Logger;

/**
 * A failsafe command executor one parameter that returns an object
 *
 * @param <A> the type of the parameter
 * @param <R> the return type
 */
public class FailsafeUnaryCommandExecutor<A, R> {

  private final Logger customLogger;
  private final String errorMessage;
  private final boolean propagateExceptions;
  private final R onFailureReturn;

  public FailsafeUnaryCommandExecutor(Logger customLogger,
                                      String errorMessage,
                                      boolean propagateExceptions,
                                      R onFailureReturn) {
    this.customLogger = customLogger;
    this.errorMessage = errorMessage;
    this.propagateExceptions = propagateExceptions;
    this.onFailureReturn = onFailureReturn;
  }

  public R execute(Function<A, R> function, A param) {
    try {
      return function.apply(param);
    } catch (Throwable e) {
      if (propagateExceptions) {
        throw e;
      }
      customLogger.warn(errorMessage, e);
    }

    return onFailureReturn;
  }


}
