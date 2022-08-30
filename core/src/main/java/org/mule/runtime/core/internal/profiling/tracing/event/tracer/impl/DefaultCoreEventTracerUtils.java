/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl;

import org.slf4j.Logger;

import java.util.function.Supplier;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utils class for {@link DefaultCoreEventTracer}
 */
public class DefaultCoreEventTracerUtils {

  private DefaultCoreEventTracerUtils() {}

  /**
   * logger used by this class.
   */
  private static final Logger LOGGER = getLogger(DefaultCoreEventTracerUtils.class);

  /**
   * Returns the value obtained from the {@param resultSupplier} or returns the {@param onFailReturnValue} if a {@link Throwable}
   * is thrown.
   *
   * @param resultSupplier      the {@link Supplier} to get the result from.
   * @param onFailReturnValue   the value to return in case a {@link Throwable} is thrown.
   * @param loggingMessage      the logging message.
   * @param propagateExceptions if the exceptions should be propagated
   * @param <T>                 the generic type of the result.
   *
   * @return the value resulted from {@param resultSupplier} or {@param onFailureReturnValue}.
   */
  public static <T> T safeExecuteWithDefaultOnThrowable(Supplier<T> resultSupplier, T onFailReturnValue, String loggingMessage,
                                                        boolean propagateExceptions) {
    try {
      return resultSupplier.get();
    } catch (Throwable e) {
      if (propagateExceptions) {
        throw e;
      }
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(loggingMessage, e);
      }

      return onFailReturnValue;
    }
  }

  /**
   * Safely executes a piece of logic.
   *
   * @param toExecute           the piece of logic to execute.
   * @param loggingMessage      the logging message if a throwable
   * @param propagateExceptions if the exceptions should be propagated
   */
  public static void safeExecute(Runnable toExecute, String loggingMessage, boolean propagateExceptions) {
    try {
      toExecute.run();
    } catch (Throwable e) {
      if (propagateExceptions) {
        throw e;
      }

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(loggingMessage, e);
      }
    }
  }

}
