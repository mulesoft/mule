/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.func;

import static org.mule.runtime.core.util.ConcurrencyUtils.withLock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Executes a given {@link CheckedRunnable} only once.
 * <p>
 * Once the {@link #runOnce()} method has been successfully executed, subsequent invokations to
 * such method will have no effect. Notice that the key word here is {@code successfully}. If the method fails,
 * each invokation to {@link #runOnce()} WILL run the delegate until it completes successfully.
 * <p>
 * Instances are thread safe, which means that if two threads are competing for the first successful invokation, only
 * one will prevail and the other one will get a no-op execution.
 *
 * @since 4.0
 */
public class Once {

  private final ReentrantLock lock = new ReentrantLock();

  private CheckedRunnable runner;
  private boolean done = false;

  /**
   * Creates a new instance
   *
   * @param runnable the delegate to be executed only once
   * @return a new instance
   */
  public static Once of(CheckedRunnable runnable) {
    return new Once(runnable);
  }

  private Once(CheckedRunnable delegate) {
    runner = () -> withLock(lock, () -> {
      if (!done) {
        delegate.run();
        done = true;
        runner = () -> {
        };
      }
    });
  }

  /**
   * Runs (or not) the delegate according to the behaviour described on the class javadoc
   */
  public void runOnce() {
    runner.run();
  }
}
