/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import static org.mule.runtime.api.exception.ExceptionHelper.getRootMuleException;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import org.slf4j.Logger;

/**
 * {@link FlowExceptionHandler} which can be used to configure a {@link MessageProcessorChain} to not handle errors, only log them.
 * This should be the case when error handling is impossible but errors should not be simply ignored.
 *
 * @since 4.0
 */
@NoExtend
public class LoggingExceptionHandler extends BaseExceptionHandler {

  private static final LoggingExceptionHandler INSTANCE = new LoggingExceptionHandler();
  private static final Logger LOGGER = getLogger(LoggingExceptionHandler.class);

  private LoggingExceptionHandler() {}

  public static FlowExceptionHandler getInstance() {
    return INSTANCE;
  }

  @Override
  protected void onError(Exception exception) {
    MuleException me = getRootMuleException(exception);
    if (me != null) {
      LOGGER.error(me.getDetailedMessage());
    } else {
      LOGGER.error("'{}: {}' has occurred.", exception.getClass().getName(), exception.getMessage(), exception);
    }
  }

  @Override
  public String toString() {
    return LoggingExceptionHandler.class.getSimpleName();
  }
}
