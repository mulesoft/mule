/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.log4j.boot.api;

import com.lmax.disruptor.ExceptionHandler;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Implementation of {@link ExceptionHandler} to be used when async loggers fail to log their messages. It will log this event
 * using the {@link StatusLogger}
 *
 * @since 4.5
 */
public class AsyncLoggerExceptionHandler implements ExceptionHandler {

  private static final StatusLogger LOGGER = StatusLogger.getLogger();

  @Override
  public void handleEventException(Throwable ex, long sequence, Object event) {
    LOGGER.error("Failed to asynchronously log message: {}", event, ex);
  }

  @Override
  public void handleOnStartException(Throwable ex) {
    LOGGER.error("Failed to start asynchronous logger", ex);
  }

  @Override
  public void handleOnShutdownException(Throwable ex) {
    LOGGER.error("Failed to stop asynchronous logger", ex);
  }
}
