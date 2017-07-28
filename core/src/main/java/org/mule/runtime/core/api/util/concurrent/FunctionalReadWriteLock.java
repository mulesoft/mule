/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.concurrent;

import static org.mule.runtime.core.internal.util.ConcurrencyUtils.withLock;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.api.util.func.CheckedSupplier;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Exposes a {@link ReentrantReadWriteLock} through a functional interface
 *
 * @since 4.0
 */
public class FunctionalReadWriteLock {

  /**
   * Functional interface for releasing a subject lock.
   */
  @FunctionalInterface
  public interface LockReleaser {

    /**
     * Releases the lock
     */
    void release();
  }


  private ReadWriteLock readWriteLock;
  private Lock readLock;
  private Lock writeLock;

  /**
   * Creates a new instance
   *
   * @return a new instance
   */
  public static FunctionalReadWriteLock readWriteLock() {
    FunctionalReadWriteLock lock = new FunctionalReadWriteLock();
    lock.readWriteLock = new ReentrantReadWriteLock();
    lock.readLock = lock.readWriteLock.readLock();
    lock.writeLock = lock.readWriteLock.writeLock();

    return lock;
  }

  private FunctionalReadWriteLock() {}

  /**
   * Executes the given function under the protection of the read lock and returns the output value.
   * <p>
   * The function receives a {@link LockReleaser} in order to release the read lock
   * in case a lock downgrade needs to be performed. If no downgrade is necessary, then there's
   * no need to manually release the lock.
   *
   * @param function the protected function
   * @param <T>      the generic type of the return value
   * @return the function's output
   */
  public <T> T withReadLock(CheckedFunction<LockReleaser, T> function) {
    final LockReleaser releaser = new DefaultLockReleaser(readLock);
    readLock.lock();
    try {
      return function.apply(releaser);
    } finally {
      releaser.release();
    }
  }

  /**
   * Executes the given supplier under the protection of the write lock and returns the
   * generated value.
   *
   * @param supplier a {@link CheckedSupplier}
   * @param <T>      the generic type of the output value
   * @return the generated value
   */
  public <T> T withWriteLock(CheckedSupplier<T> supplier) {
    return withLock(writeLock, supplier);
  }

  /**
   * Executes the given supplier under the protection of the write lock.
   *
   * @param runnable a {@link CheckedRunnable}
   */
  public void withWriteLock(CheckedRunnable runnable) {
    withLock(writeLock, runnable);
  }

  private final class DefaultLockReleaser implements LockReleaser {

    private final Lock lock;
    private boolean acquired = true;

    private DefaultLockReleaser(Lock lock) {
      this.lock = lock;
    }

    @Override
    public void release() {
      if (acquired) {
        try {
          lock.unlock();
        } finally {
          acquired = false;
        }
      }
    }
  }
}
