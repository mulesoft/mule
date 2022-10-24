/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;

/**
 * {@link LockGroup} implementation for holding references to created locks inside a mule instance.
 */
public class InstanceLockGroup implements LockGroup {

  private static final Logger LOGGER = LoggerFactory.getLogger(InstanceLockGroup.class);

  private final Map<String, LockEntry> locks;
  private final Object lockAccessMonitor = new Object();
  private final LockProvider lockProvider;
  private final long gracefulShutdownTimeoutMillis;

  public InstanceLockGroup(LockProvider lockProvider, long shutdownTimeoutMillis) {
    this.lockProvider = lockProvider;
    this.locks = new HashMap<>();
    this.gracefulShutdownTimeoutMillis = shutdownTimeoutMillis;
  }

  @Override
  public void lock(String lockId) {
    LockEntry lockEntry = getOrCreateLockEntry(lockId);
    lockEntry.getLock().lock();
  }

  @Override
  public void unlock(String key) {
    synchronized (lockAccessMonitor) {
      LockEntry lockEntry = locks.get(key);
      if (lockEntry != null) {
        lockEntry.getLock().unlock();
        releaseLockEntry(key, lockEntry);
      } else {
        LOGGER.warn("Trying to unlock a lock with id {} that wasn't previously locked", key);
      }
    }
  }

  @Override
  public boolean tryLock(String lockId, long timeout, TimeUnit timeUnit) throws InterruptedException {
    LockEntry lockEntry = getOrCreateLockEntry(lockId);
    try {
      boolean lockAcquired = lockEntry.getLock().tryLock(timeout, timeUnit);
      if (!lockAcquired) {
        releaseLockEntry(lockId, lockEntry);
      }
      return lockAcquired;
    } catch (InterruptedException interruptedException) {
      releaseLockEntry(lockId, lockEntry);
      throw interruptedException;
    }
  }

  @Override
  public boolean tryLock(String lockId) {
    LockEntry lockEntry = getOrCreateLockEntry(lockId);
    boolean lockAcquired = lockEntry.getLock().tryLock();
    if (!lockAcquired) {
      releaseLockEntry(lockId, lockEntry);
    }
    return lockAcquired;
  }

  @Override
  public void lockInterruptibly(String lockId) throws InterruptedException {
    LockEntry lockEntry = getOrCreateLockEntry(lockId);
    try {
      lockEntry.getLock().lockInterruptibly();
    } catch (InterruptedException e) {
      releaseLockEntry(lockId, lockEntry);
      throw e;
    }
  }

  public static class LockEntry {

    private final AtomicInteger lockCount = new AtomicInteger(0);
    private final Lock lock;

    public LockEntry(Lock lock) {
      this.lock = lock;
    }

    public Lock getLock() {
      return lock;
    }

    public void incrementLockCount() {
      lockCount.incrementAndGet();
    }

    public void decrementLockCount() {
      lockCount.decrementAndGet();
    }

    public boolean hasPendingLocks() {
      return lockCount.get() > 0;
    }
  }

  @Override
  public void dispose() {
    waitForLocksToBeUnlocked();
  }

  private LockEntry getOrCreateLockEntry(String lockId) {
    LockEntry lockEntry;
    synchronized (lockAccessMonitor) {
      if (locks.containsKey(lockId)) {
        lockEntry = locks.get(lockId);
      } else {
        lockEntry = new LockEntry(lockProvider.createLock(lockId));
        locks.put(lockId, lockEntry);
      }
      lockEntry.incrementLockCount();
    }
    return lockEntry;
  }

  private void releaseLockEntry(String lockId, LockEntry lockEntry) {
    synchronized (lockAccessMonitor) {
      lockEntry.decrementLockCount();
      if (!lockEntry.hasPendingLocks()) {
        locks.remove(lockId);
        if (locks.isEmpty()) {
          lockAccessMonitor.notifyAll();
        }
      }
    }
  }

  private void waitForLocksToBeUnlocked() {
    long beginMillis = currentTimeMillis();
    synchronized (lockAccessMonitor) {
      try {
        boolean isTimeout = false;
        while (!locks.isEmpty() && !isTimeout) {
          long elapsedMillis = currentTimeMillis() - beginMillis;
          long remainingMillis = gracefulShutdownTimeoutMillis - elapsedMillis;
          if (remainingMillis > 0) {
            lockAccessMonitor.wait(remainingMillis);
          } else {
            isTimeout = true;
          }
        }
      } catch (InterruptedException e) {
        currentThread().interrupt();
      }
      if (!locks.isEmpty()) {
        LOGGER.warn("These locks weren't unlocked before disposing its lock group: {}", locks.keySet());
      }
    }
  }
}
