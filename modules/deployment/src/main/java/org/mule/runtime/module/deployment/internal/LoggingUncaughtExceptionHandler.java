/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  public void uncaughtException(Thread thread, Throwable throwable) {
    logger.error(String.format("Uncaught exception in %s%n%n", thread), throwable);
  }
}
