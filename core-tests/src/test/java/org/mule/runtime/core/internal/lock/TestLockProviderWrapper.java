/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lock;

import static org.mockito.Mockito.spy;

import org.mule.runtime.api.lock.LockProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * An implementation of {@link LockProvider} that includes some test capabilities, such as:
 * <ul>
 * <li>Spy the provided locks and get them by using the method {@link #getSpiedLock(String)}.</li>
 * <li>Make those provided locks to raise exceptions for the methods that can do that, by calling the method
 * {@link #makeLocksRaiseExceptions()}. The exceptions would be:</li>
 * <ul>
 * <li>{@link Lock#lockInterruptibly()} will raise a {@link InterruptedException}</li>
 * <li>{@link Lock#tryLock(long, TimeUnit)} will raise a {@link InterruptedException}</li>
 * <li>{@link Lock#unlock()} will raise a {@link IllegalMonitorStateException}</li>
 * </ul>
 * </ul>
 */
class TestLockProviderWrapper implements LockProvider {

  private final Map<String, LockWrapper> providedLocks = new HashMap<>();
  private final LockProvider delegate;
  private boolean newLocksShouldRaiseExceptions = false;

  public TestLockProviderWrapper(LockProvider delegate) {
    this.delegate = delegate;
  }

  @Override
  public Lock createLock(String lockId) {
    Lock actualLock = delegate.createLock(lockId);
    LockWrapper wrappedLock = new LockWrapper(actualLock, newLocksShouldRaiseExceptions);
    LockWrapper spiedLock = spy(wrappedLock);
    providedLocks.put(lockId, spiedLock);
    return spiedLock;
  }

  public Lock getSpiedLock(String lockId) {
    return providedLocks.get(lockId);
  }

  public void makeLocksRaiseExceptions() {
    newLocksShouldRaiseExceptions = true;
    for (LockWrapper lock : providedLocks.values()) {
      lock.raiseExceptions();
    }
  }

  private static class LockWrapper implements Lock {

    private final Lock actual;
    private boolean shouldRaiseExceptions;

    LockWrapper(Lock actual, boolean shouldRaiseExceptions) {
      this.actual = actual;
      this.shouldRaiseExceptions = shouldRaiseExceptions;
    }

    void raiseExceptions() {
      shouldRaiseExceptions = true;
    }

    @Override
    public void lock() {
      actual.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
      if (shouldRaiseExceptions) {
        throw new InterruptedException();
      }
      actual.lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
      return actual.tryLock();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
      if (shouldRaiseExceptions) {
        throw new InterruptedException();
      }
      return actual.tryLock(time, unit);
    }

    @Override
    public void unlock() {
      if (shouldRaiseExceptions) {
        throw new IllegalMonitorStateException();
      }
      actual.unlock();
    }

    @Override
    public Condition newCondition() {
      return actual.newCondition();
    }
  }
}
