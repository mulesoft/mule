/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

/**
 * {@link LockGroup} implementation for holding references to created locks inside a mule instance.
 */
public class InstanceLockGroup implements LockGroup {

  private Map<String, LockEntry> locks;
  private Object lockAccessMonitor = new Object();
  private LockProvider lockProvider;

  public InstanceLockGroup(LockProvider lockProvider) {
    this.lockProvider = lockProvider;
    this.locks = new HashMap<>();
  }

  @Override
  public void lock(String lockId) {
    LockEntry lockEntry;
    synchronized (lockAccessMonitor) {
      if (locks.containsKey(lockId)) {
        lockEntry = locks.get(lockId);
      } else {
        lockEntry = new LockEntry(lockProvider.createLock(lockId));
        locks.put(lockId, lockEntry);
      }
      lockEntry.incrementLockCount();
      lockAccessMonitor.notifyAll();
    }
    lockEntry.getLock().lock();
  }

  @Override
  public void unlock(String key) {
    synchronized (lockAccessMonitor) {
      LockEntry lockEntry = locks.get(key);
      if (lockEntry != null) {
        lockEntry.decrementLockCount();
        if (!lockEntry.hasPendingLocks()) {
          locks.remove(key);
        }
        lockEntry.getLock().unlock();
      }
      lockAccessMonitor.notifyAll();
    }
  }

  @Override
  public boolean tryLock(String lockId, long timeout, TimeUnit timeUnit) throws InterruptedException {
    LockEntry lockEntry;
    synchronized (lockAccessMonitor) {
      if (locks.containsKey(lockId)) {
        lockEntry = locks.get(lockId);
      } else {
        lockEntry = new LockEntry(lockProvider.createLock(lockId));
        locks.put(lockId, lockEntry);
      }
      lockEntry.incrementLockCount();
      lockAccessMonitor.notifyAll();
    }
    boolean lockAcquired = lockEntry.getLock().tryLock(timeout, timeUnit);
    if (!lockAcquired) {
      synchronized (lockAccessMonitor) {
        lockEntry.decrementLockCount();
        if (!lockEntry.hasPendingLocks()) {
          locks.remove(lockId);
        }
      }
    }
    return lockAcquired;
  }

  @Override
  public boolean tryLock(String lockId) {
    LockEntry lockEntry;
    synchronized (lockAccessMonitor) {
      if (locks.containsKey(lockId)) {
        lockEntry = locks.get(lockId);
      } else {
        lockEntry = new LockEntry(lockProvider.createLock(lockId));
        locks.put(lockId, lockEntry);
      }
      lockEntry.incrementLockCount();
      lockAccessMonitor.notifyAll();
    }
    boolean lockAcquired = lockEntry.getLock().tryLock();
    if (!lockAcquired) {
      synchronized (lockAccessMonitor) {
        lockEntry.decrementLockCount();
        if (!lockEntry.hasPendingLocks()) {
          locks.remove(lockId);
        }
      }
    }
    return lockAcquired;
  }

  @Override
  public void lockInterruptibly(String lockId) throws InterruptedException {
    LockEntry lockEntry;
    synchronized (lockAccessMonitor) {
      if (locks.containsKey(lockId)) {
        lockEntry = locks.get(lockId);
      } else {
        lockEntry = new LockEntry(lockProvider.createLock(lockId));
        locks.put(lockId, lockEntry);
      }
      lockEntry.incrementLockCount();
      lockAccessMonitor.notifyAll();
    }
    lockEntry.getLock().lockInterruptibly();
  }

  public static class LockEntry {

    private AtomicInteger lockCount = new AtomicInteger(0);
    private Lock lock;

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
    synchronized (lockAccessMonitor) {
      locks.clear();
    }
  }
}
