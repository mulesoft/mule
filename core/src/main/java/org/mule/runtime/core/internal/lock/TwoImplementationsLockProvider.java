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
import org.mule.runtime.api.util.concurrent.Latch;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
      return new TwoImplementationCondition(lock1.newCondition(), lock2.newCondition());
    }
  }

  private final class TwoImplementationCondition implements Condition {

    private final Condition cond1;
    private final Condition cond2;

    private TwoImplementationCondition(Condition cond1, Condition cond2) {
      this.cond1 = cond1;
      this.cond2 = cond2;
    }

    private Thread awaitThread(AtomicInteger waitingFor, AtomicBoolean interrupted, AtomicBoolean acquired, Latch latch,
                               Awaiter awaiting) {
      return new Thread(() -> {
        try {
          if (awaiting.await()) {
            acquired.set(true);
          }
          if (waitingFor.decrementAndGet() == 0) {
            latch.release();
          }
        } catch (InterruptedException e) {
          interrupted.set(true);
          if (waitingFor.decrementAndGet() == 0) {
            latch.release();
          }
        }
      });
    }

    @Override
    public void await() throws InterruptedException {
      AtomicInteger waitingFor = new AtomicInteger(2);
      AtomicBoolean interrupted = new AtomicBoolean(false);
      Latch latch = new Latch();
      awaitThread(waitingFor, interrupted, new AtomicBoolean(), latch, () -> {
        cond1.await();
        return true;
      }).start();
      awaitThread(waitingFor, interrupted, new AtomicBoolean(), latch, () -> {
        cond2.await();
        return true;
      }).start();
      latch.await();
      if (interrupted.get()) {
        throw new InterruptedException();
      }
    }

    @Override
    public void awaitUninterruptibly() {
      AtomicInteger waitingFor = new AtomicInteger(2);
      Latch latch = new Latch();
      awaitThread(waitingFor, new AtomicBoolean(), new AtomicBoolean(), latch, () -> {
        cond1.awaitUninterruptibly();
        return true;
      }).start();
      awaitThread(waitingFor, new AtomicBoolean(), new AtomicBoolean(), latch, () -> {
        cond2.awaitUninterruptibly();
        return true;
      }).start();
      try {
        latch.await();
      } catch (InterruptedException e) {
        // do nothing
      }
    }

    @Override
    public long awaitNanos(long nanosTimeout) throws InterruptedException {
      AtomicInteger waitingFor = new AtomicInteger(2);
      AtomicBoolean interrupted = new AtomicBoolean(false);
      Latch latch = new Latch();
      long start = System.currentTimeMillis() * 1000;
      awaitThread(waitingFor, interrupted, new AtomicBoolean(), latch, () -> {
        cond1.awaitNanos(nanosTimeout);
        return true;
      }).start();
      awaitThread(waitingFor, interrupted, new AtomicBoolean(), latch, () -> {
        cond2.awaitNanos(nanosTimeout);
        return true;
      }).start();
      latch.await();
      long spent = System.currentTimeMillis() * 1000 - start;
      if (interrupted.get()) {
        throw new InterruptedException();
      }
      return nanosTimeout - spent;
    }

    @Override
    public boolean await(long time, TimeUnit unit) throws InterruptedException {
      AtomicInteger waitingFor = new AtomicInteger(2);
      AtomicBoolean interrupted = new AtomicBoolean(false);
      AtomicBoolean acq1 = new AtomicBoolean(false);
      AtomicBoolean acq2 = new AtomicBoolean(false);
      Latch latch = new Latch();
      awaitThread(waitingFor, interrupted, acq1, latch, () -> cond1.await(time, unit)).start();
      awaitThread(waitingFor, interrupted, acq2, latch, () -> cond2.await(time, unit)).start();
      latch.await();
      if (interrupted.get()) {
        throw new InterruptedException();
      }
      return acq1.get() && acq2.get();
    }

    @Override
    public boolean awaitUntil(Date deadline) throws InterruptedException {
      AtomicInteger waitingFor = new AtomicInteger(2);
      AtomicBoolean interrupted = new AtomicBoolean(false);
      AtomicBoolean acq1 = new AtomicBoolean(false);
      AtomicBoolean acq2 = new AtomicBoolean(false);
      Latch latch = new Latch();
      awaitThread(waitingFor, interrupted, acq1, latch, () -> cond1.awaitUntil(deadline)).start();
      awaitThread(waitingFor, interrupted, acq2, latch, () -> cond2.awaitUntil(deadline)).start();
      latch.await();
      if (interrupted.get()) {
        throw new InterruptedException();
      }
      return acq1.get() && acq2.get();
    }

    @Override
    public void signal() {
      cond1.signal();
      cond2.signal();
    }

    @Override
    public void signalAll() {
      cond1.signalAll();
      cond2.signalAll();
    }
  }

  private interface Awaiter {

    boolean await() throws InterruptedException;
  }
}
