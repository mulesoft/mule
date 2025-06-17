/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.span.command;

import org.apache.commons.lang3.function.TriFunction;
import org.slf4j.Logger;

/**
 * A failsafe command executor with three parameters that returns an object
 *
 * @param <R> the return type
 * @param <A> the type of the first parameter
 * @param <B> the type of the second parameter.
 * @param <C> the type of the third parameter.
 */
public class FailsafeTriCommandExecutor<R, A, B, C> {

  private final Logger customLogger;
  private final String errorMessage;
  private final boolean propagateExceptions;
  private final R onFailReturnValue;

  public FailsafeTriCommandExecutor(Logger customLogger,
                                    String errorMessage,
                                    boolean propagateExceptions,
                                    R onFailReturnValue) {
    this.customLogger = customLogger;
    this.errorMessage = errorMessage;
    this.propagateExceptions = propagateExceptions;
    this.onFailReturnValue = onFailReturnValue;
  }

  public R execute(TriFunction<A, B, C, R> function,
                   A firstParameter,
                   B secondParameter,
                   C thirdParameter) {
    try {
      return function.apply(firstParameter, secondParameter, thirdParameter);
    } catch (Throwable e) {
      if (propagateExceptions) {
        throw e;
      }
      customLogger.warn(errorMessage, e);

      return onFailReturnValue;
    }
  }

}
