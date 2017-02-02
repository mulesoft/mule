/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Implementation of {@link Lock} that delegates the locking mechanism to a {@link LockGroup} but looks like a regular lock from
 * the client's perspective
 */
public class LockAdapter implements Lock {

  private LockGroup lockGroup;
  private String lockId;

  public LockAdapter(String lockId, LockGroup lockGroup) {
    this.lockGroup = lockGroup;
    this.lockId = lockId;
  }

  @Override
  public void lock() {
    lockGroup.lock(lockId);
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    lockGroup.lockInterruptibly(lockId);
  }

  @Override
  public boolean tryLock() {
    return lockGroup.tryLock(lockId);
  }

  @Override
  public boolean tryLock(long timeout, TimeUnit timeUnit) throws InterruptedException {
    return lockGroup.tryLock(lockId, timeout, timeUnit);
  }

  @Override
  public void unlock() {
    lockGroup.unlock(lockId);
  }

  @Override
  public Condition newCondition() {
    throw new UnsupportedOperationException("Operation not supported by mule locks");
  }
}
