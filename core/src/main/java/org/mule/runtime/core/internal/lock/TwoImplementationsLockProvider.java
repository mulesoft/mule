/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lock;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.runtime.api.lock.LockProvider;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * A Combined implementation of 2 {@link LockProvider}s that uses both locks to lock. This is only meant to be used for migration
 * of HA implementation from one Distributed Lock mechanism to another one, using this as a mid-step, to achieve 0 downtime.
 *
 * @since 4.10
 */
public class TwoImplementationsLockProvider implements LockProvider {

  private final LockProvider prov1;
  private final LockProvider prov2;

  public TwoImplementationsLockProvider(LockProvider prov1, LockProvider prov2) {
    this.prov1 = prov1;
    this.prov2 = prov2;
  }

  @Override
  public Lock createLock(String lockId) {
    return new TwoImplementationLock(prov1.createLock(lockId), prov2.createLock(lockId));
  }

  private final class TwoImplementationLock implements Lock {

    private final Lock lock1;
    private final Lock lock2;

    private TwoImplementationLock(Lock lock1, Lock lock2) {
      this.lock1 = lock1;
      this.lock2 = lock2;
    }

    @Override
    public void lock() {
      this.lock1.lock();
      this.lock2.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
      this.lock1.lockInterruptibly();
      try {
        this.lock2.lockInterruptibly();
      } catch (InterruptedException e) {
        this.lock1.unlock();
        throw e;
      }
    }

    @Override
    public boolean tryLock() {
      if (!lock1.tryLock()) {
        return false;
      }
      if (!lock2.tryLock()) {
        lock1.unlock();
        return false;
      }
      return true;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
      long start = currentTimeMillis();
      if (!lock1.tryLock(time, unit)) {
        return false;
      }
      long passed = currentTimeMillis() - start;
      if (!lock2.tryLock(time - unit.convert(passed, MILLISECONDS), unit)) {
        lock1.unlock();
        return false;
      }
      return true;
    }

    @Override
    public void unlock() {
      lock2.unlock();
      lock1.unlock();
    }

    @Override
    public Condition newCondition() {
      throw new UnsupportedOperationException("Operation not supported by mule locks");
    }
  }
}
