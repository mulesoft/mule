/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.source.poll;


import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;

/**
 * {@link Runnable} that delegates its execution. The delegate can be switch at any time. This is particulary useful when you want
 * to schedule a task to be repeated, and you want to change the task for the next executions without rescheduling.
 *
 * @since 4.2.3 4.3.1 4.4.0
 */
public class DelegateRunnable implements Runnable {

  private static final Logger LOGGER = getLogger(DelegateRunnable.class);

  private Runnable delegate;
  private Lock lock = new ReentrantLock();

  public DelegateRunnable(Runnable delegate) {
    this.delegate = delegate;
  }

  @Override
  public void run() {
    // We want to make sure that the delegate value is not changed when executing it.
    if (lock.tryLock()) {
      try {
        if (delegate != null) {
          delegate.run();
        } else {
          LOGGER.debug("The execution of the task was skipped because no delegate was set.");
        }
      } finally {
        lock.unlock();
      }
    }
  }

  public void setDelegate(Runnable delegate) {
    lock.lock();
    this.delegate = delegate;
    lock.unlock();
  }
}
