/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.withLifecycleListener;

/**
 * Thread for testing purposes. The idea is to retain the Thread's Context ClassLoader until the Thread is terminated.
 */
public class LeakedThread extends Thread {

  private boolean stopRequested = false;

  public synchronized void run() {
    stopRequested = false;
    while (!stopRequested) {
      try {
        wait();
      } catch (InterruptedException e) {
        // does nothing (keeps waiting)
      }
    }
  }

  public synchronized void stopPlease() {
    stopRequested = true;
    notify();
  }
}
