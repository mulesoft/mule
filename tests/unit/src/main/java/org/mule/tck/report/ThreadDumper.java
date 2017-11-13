/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.report;

import static java.lang.System.lineSeparator;
import static java.lang.management.ManagementFactory.getThreadMXBean;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import java.lang.management.ThreadMXBean;

/**
 * Provides a way to generate thread dumps in order to troubleshoot tests.
 *
 * @since 4.1
 */
public class ThreadDumper {

  private static final ThreadMXBean tmx = getThreadMXBean();

  private static final Logger LOGGER = getLogger(ThreadDumper.class);

  /**
   * Call this method from your application whenever you want to log a thread dump.
   */
  public static void logThreadDump() {
    LOGGER
        .error(lineSeparator() + stream(tmx.dumpAllThreads(true, true)).map(Object::toString).collect(joining(lineSeparator())));
  }

}
