/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
