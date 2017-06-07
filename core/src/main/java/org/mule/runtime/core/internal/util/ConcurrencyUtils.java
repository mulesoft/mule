/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.api.util.func.CheckedSupplier;

import java.util.concurrent.locks.Lock;

/**
 * Utilities for concurrency
 *
 * @since 4.0
 */
public class ConcurrencyUtils {

  /**
   * Safely releases the given {@code lock} without failing if it is in an illegal state
   *
   * @param lock a {@link Lock}
   */
  public static void safeUnlock(Lock lock) {
    try {
      lock.unlock();
    } catch (IllegalMonitorStateException e) {
      // lock was released early to improve performance and somebody else took it. This is fine
    }
  }

  /**
   * Returns the value of the given {@code supplier} between the boundaries of
   * the given {@code lock}. It guarantees that the lock is released
   *
   * @param lock     a {@link Lock}
   * @param supplier a {@link CheckedSupplier}
   * @param <T>      the generic type of the returned value
   * @return the supplied value
   */
  public static <T> T withLock(Lock lock, CheckedSupplier<T> supplier) {
    lock.lock();
    try {
      return supplier.get();
    } finally {
      safeUnlock(lock);
    }
  }

  /**
   * Execute the given {@code delegate} between the boundaries of
   * the given {@code lock}. It guarantees that the lock is released
   *
   * @param lock     a {@link Lock}
   * @param delegate a {@link CheckedRunnable}
   */
  public static void withLock(Lock lock, CheckedRunnable delegate) {
    lock.lock();
    try {
      delegate.run();
    } finally {
      safeUnlock(lock);
    }
  }

  private ConcurrencyUtils() {}
}
