/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
 * Rule that generates a thread dump on failure.
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
      if (LOGGER.isTraceEnabled()) {
        logThreadDump();
      }
    }
  }
}
