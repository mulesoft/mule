/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.withLifecycleListener;

/**
 * Thread that retains a reference to a given {@link ClassLoader} until it finishes executing. For testing purposes.
 */
public class LeakingThread extends Thread {

  private final ClassLoader classLoaderToLeak;
  private boolean stopRequested = false;

  public LeakingThread(ClassLoader classLoaderToLeak) {
    this.classLoaderToLeak = classLoaderToLeak;
  }

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
