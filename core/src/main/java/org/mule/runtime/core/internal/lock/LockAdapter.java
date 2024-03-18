/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lock;

import org.mule.runtime.api.scheduler.SchedulerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Implementation of {@link Lock} that delegates the locking mechanism to a {@link LockGroup} but looks like a regular lock from
 * the client's perspective
 */
public class LockAdapter implements Lock {

  private static final boolean CHECK_LOCKABLE = Boolean.getBoolean(LockAdapter.class.getName() + ".CHECK_LOCKABLE");
  private static final Logger LOGGER = LoggerFactory.getLogger(LockAdapter.class);

  private LockGroup lockGroup;
  private String lockId;
  private SchedulerService schedulerService;

  public LockAdapter(String lockId, LockGroup lockGroup, SchedulerService schedulerService) {
    this.lockGroup = lockGroup;
    this.lockId = lockId;
    this.schedulerService = schedulerService;
  }

  @Override
  public void lock() {
    checkLockableThread();
    lockGroup.lock(lockId);
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    checkLockableThread();
    lockGroup.lockInterruptibly(lockId);
  }

  private void checkLockableThread() {
    if (CHECK_LOCKABLE && schedulerService.isCurrentThreadForCpuWork()) {
      LOGGER
          .warn("About to lock current thread, which is not waitAllowed. Either dispatch the work to a waitAllowed Scheduler or use `tryLock`",
                new NonWaitAllowedThreadBlocked());
    }
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

  private static class NonWaitAllowedThreadBlocked extends RuntimeException {

    private static final long serialVersionUID = 123139630158524821L;

    public NonWaitAllowedThreadBlocked() {
      super("An exception is logged so the stack trace that leads to this situation can be traced.");
    }
  }
}
