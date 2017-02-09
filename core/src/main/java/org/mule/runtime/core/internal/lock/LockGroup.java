/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lock;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lock.LockFactory;

import java.util.concurrent.TimeUnit;

/**
 * Holds reference to all the obtained locks using {@link LockFactory} in order to release memory of no longer referenced locks.
 *
 */
public interface LockGroup extends Disposable {

  /*
   * Gets a lock over the resource identified with lockId
   */
  void lock(String lockId);

  /*
   * Releases lock over the resource identified with lockId
   */
  void unlock(String lockId);

  /**
   * Tries to acquire the lock for a certain amount of time
   *
   * @param timeout the time in timeUnit to wait until the lock is acquired
   * @param timeUnit the time unit of timeout
   * @return true if the lock was successfully acquired, false otherwise
   * @throws java.lang.InterruptedException if thread was interrupted during the lock acquisition
   */
  boolean tryLock(String lockId, long timeout, TimeUnit timeUnit) throws InterruptedException;

  boolean tryLock(String lockId);

  void lockInterruptibly(String lockId) throws InterruptedException;
}
