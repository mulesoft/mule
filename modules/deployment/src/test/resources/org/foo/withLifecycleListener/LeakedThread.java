/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.withLifecycleListener;

import java.util.concurrent.CountDownLatch;

/**
 * Thread for testing purposes. The idea is to retain the Thread's Context ClassLoader until the Thread is terminated.
 */
public class LeakedThread extends Thread {

  private final CountDownLatch latch = new CountDownLatch(1);

  public void run() {
    try {
      latch.await();
    } catch (InterruptedException e) {
      // does nothing
    }
  }

  public void stopPlease() {
    latch.countDown();
  }
}
