/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util;

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

  private ConcurrencyUtils() {}
}
