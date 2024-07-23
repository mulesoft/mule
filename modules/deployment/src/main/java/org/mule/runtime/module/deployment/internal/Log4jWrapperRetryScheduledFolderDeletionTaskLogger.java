/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.slf4j.Logger;

/**
 * A Logger for the {@link RetryScheduledFolderDeletionTask} that wraps a log4j logger. This is only used for 4.4.x.
 */
public class Log4jWrapperRetryScheduledFolderDeletionTaskLogger implements RetryScheduledFolderDeletionTaskLogger {

  private final Logger logger;

  public Log4jWrapperRetryScheduledFolderDeletionTaskLogger(Logger logger) {
    this.logger = logger;
  }

  @Override
  public void debug(String template, Object... parameters) {
    logger.debug(template, parameters);
  }

  @Override
  public void error(String template, Object... parameters) {
    logger.error(template, parameters);
  }

  @Override
  public void info(String template, Object... parameters) {
    logger.info(template, parameters);
  }

  @Override
  public void warn(String template, Object... parameters) {
    logger.warn(template, parameters);
  }
}
