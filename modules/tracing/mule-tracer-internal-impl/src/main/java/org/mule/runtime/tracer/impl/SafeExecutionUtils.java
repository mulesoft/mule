/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl;

import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Utils class.
 */
public class SafeExecutionUtils {

  private SafeExecutionUtils() {}

  /**
   * Returns the value obtained from the {@param resultSupplier} or returns the {@param onFailReturnValue} if a {@link Throwable}
   * is thrown.
   *
   * @param resultSupplier      the {@link Supplier} to get the result from.
   * @param onFailReturnValue   the value to return in case a {@link Throwable} is thrown.
   * @param loggingMessage      the logging message.
   * @param propagateExceptions if the exceptions should be propagated
   * @param logger              logger used for informing tracing errors.
   * @param <T>                 the generic type of the result.
   *
   * @return the value resulted from {@param resultSupplier} or {@param onFailureReturnValue}.
   */
  public static <T> T safeExecuteWithDefaultOnThrowable(Supplier<T> resultSupplier, T onFailReturnValue, String loggingMessage,
                                                        boolean propagateExceptions,
                                                        Logger logger) {
    try {
      return resultSupplier.get();
    } catch (Throwable e) {
      if (propagateExceptions) {
        throw e;
      }
      logger.warn(loggingMessage, e);

      return onFailReturnValue;
    }
  }

  /**
   * Safely executes a piece of logic.
   *
   * @param toExecute           the piece of logic to execute.
   * @param loggingMessage      the logging message if a throwable
   * @param propagateExceptions if the exceptions should be propagated
   * @param logger              logger used for informing tracing errors.
   */
  public static void safeExecute(Runnable toExecute, String loggingMessage, boolean propagateExceptions, Logger logger) {
    try {
      toExecute.run();
    } catch (Throwable e) {
      if (propagateExceptions) {
        throw e;
      }

      logger.warn(loggingMessage, e);
    }
  }

}
