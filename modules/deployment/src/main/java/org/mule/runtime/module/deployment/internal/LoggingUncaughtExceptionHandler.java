/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
