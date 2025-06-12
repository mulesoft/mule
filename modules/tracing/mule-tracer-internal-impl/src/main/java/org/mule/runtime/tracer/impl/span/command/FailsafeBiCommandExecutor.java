/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.span.command;

import java.util.function.BiConsumer;

import org.slf4j.Logger;

/**
 * A failsafe command executor with two parameters.
 *
 * @param <A> the type of the first parameter
 * @param <B> the type of the second parameter.
 */
public class FailsafeBiCommandExecutor<A, B> {

  private final Logger customLogger;
  private final String errorMessage;
  private final boolean propagateExceptions;

  public FailsafeBiCommandExecutor(Logger customLogger,
                                   String errorMessage,
                                   boolean propagateExceptions) {
    this.customLogger = customLogger;
    this.errorMessage = errorMessage;
    this.propagateExceptions = propagateExceptions;
  }

  public void execute(BiConsumer<A, B> consumer, A firstParameter,
                      B secondParameter) {
    try {
      consumer.accept(firstParameter, secondParameter);
    } catch (Throwable e) {
      if (propagateExceptions) {
        throw e;
      }
      customLogger.warn(errorMessage, e);
    }
  }

}
