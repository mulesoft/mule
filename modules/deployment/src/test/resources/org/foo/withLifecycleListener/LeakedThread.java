/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.withLifecycleListener;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;

/**
 * Thread for testing purposes. The idea is to retain the Thread's Context ClassLoader until the Thread is terminated.
 */
public class LeakedThread extends Thread {

  private static Logger LOGGER = getLogger(LeakedThread.class);

  private final CountDownLatch latch = new CountDownLatch(1);

  public void run() {
    LOGGER.error("[EZE] LeakedThread.run - Started");
    try {
      LOGGER.error("[EZE] LeakedThread.run - Waiting");
      latch.await();
      LOGGER.error("[EZE] LeakedThread.run - Notified");
      return;
    } catch (InterruptedException e) {
      LOGGER.error("[EZE] LeakedThread.run - Thread interrupted");
      // does nothing (keeps waiting)
    }
  }

  public void stopPlease() {
    LOGGER.error("[EZE] LeakedThread.stopPlease");
    latch.countDown();
    LOGGER.error("[EZE] LeakedThread.stopPlease - Notified");
  }
}
