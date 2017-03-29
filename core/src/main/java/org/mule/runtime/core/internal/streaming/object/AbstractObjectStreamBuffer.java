/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.util.FunctionalUtils.safely;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.core.util.func.CheckedRunnable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;

/**
 * Base class for implementations of {@link ObjectStreamBuffer}
 *
 * @param <T> the generic type of the items in the stream
 * @since 4.0
 */
public abstract class AbstractObjectStreamBuffer<T> implements ObjectStreamBuffer<T> {

  private static Logger LOGGER = getLogger(AbstractObjectStreamBuffer.class);

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  protected final Lock readLock = readWriteLock.readLock();
  protected final Lock writeLock = readWriteLock.writeLock();

  @Override
  public final T get(long position) {
    checkNotClosed();
    return doGet(position);
  }

  protected abstract T doGet(long position);

  @Override
  public final boolean hasNext(long position) {
    if (closed.get()) {
      return false;
    }

    return doHasNext(position);
  }

  protected abstract boolean doHasNext(long position);

  @Override
  public final void close() {
    if (closed.compareAndSet(false, true)) {
      writeLock.lock();
      try {
        doClose();
      } finally {
        writeLock.unlock();
      }
    }
  }

  protected abstract void doClose();

  private void checkNotClosed() {
    checkState(!closed.get(), "Buffer is closed");
  }

  protected void closeSafely(CheckedRunnable task) {
    safely(task, e -> LOGGER.debug("Found exception closing buffer", e));
  }
}
