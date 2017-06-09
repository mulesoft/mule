/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.internal.util.ConcurrencyUtils.withLock;
import static org.mule.runtime.core.internal.util.FunctionalUtils.safely;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.streaming.Cursor;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.api.util.func.CheckedSupplier;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;

/**
 * Base class for streaming buffers with basic functionality to allow {@link Cursor cursors}
 * to have concurrent access to the stream's content
 *
 * @since 4.0
 */
public abstract class AbstractStreamingBuffer {

  private static Logger LOGGER = getLogger(AbstractStreamingBuffer.class);

  protected final AtomicBoolean closed = new AtomicBoolean(false);
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  protected final Lock readLock = readWriteLock.readLock();
  protected final Lock writeLock = readWriteLock.writeLock();

  protected <T> T withReadLock(CheckedFunction<LockReleaser, T> function) {
    final LockReleaser releaser = new LockReleaser(readLock);
    readLock.lock();
    try {
      return function.apply(releaser);
    } finally {
      releaser.release();
    }
  }

  protected <T> T withWriteLock(CheckedSupplier<T> supplier) {
    return withLock(writeLock, supplier);
  }

  protected void checkNotClosed() {
    checkState(!closed.get(), "Buffer is closed");
  }

  protected void closeSafely(CheckedRunnable task) {
    safely(task, e -> LOGGER.debug("Found exception closing buffer", e));
  }

  public final class LockReleaser {

    private final Lock lock;
    private boolean acquired = true;

    private LockReleaser(Lock lock) {
      this.lock = lock;
    }

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
