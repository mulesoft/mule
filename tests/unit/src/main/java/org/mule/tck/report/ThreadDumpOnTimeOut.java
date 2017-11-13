/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.report;

import static org.mule.tck.report.ThreadDumper.logThreadDump;
import static org.slf4j.LoggerFactory.getLogger;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.TestTimedOutException;
import org.slf4j.Logger;

import java.util.concurrent.TimeoutException;

/**
 * Rule that generates a heap dump on failure.
 * <p>
 * Useful for troubleshooting failures in memory leak prevention tests.
 *
 * @since 4.1
 */
public class ThreadDumpOnTimeOut extends TestWatcher {

  private static final Logger LOGGER = getLogger(ThreadDumpOnTimeOut.class);

  @Override
  protected void failed(Throwable e, Description description) {
    super.failed(e, description);

    if (e instanceof TestTimedOutException || e instanceof TimeoutException) {
      LOGGER.error("test timed out. Maybe due to a deadlock?");
      logThreadDump();
    }
  }
}
