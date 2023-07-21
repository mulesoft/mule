/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming;

import static org.mule.runtime.api.util.Preconditions.checkState;

import org.mule.runtime.api.streaming.Cursor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Base class for streaming buffers with basic functionality to allow {@link Cursor cursors} to have concurrent access to the
 * stream's content
 *
 * @since 4.0
 */
public abstract class AbstractStreamingBuffer {

  protected final AtomicBoolean closed = new AtomicBoolean(false);
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  protected final Lock readLock = readWriteLock.readLock();
  protected final Lock writeLock = readWriteLock.writeLock();

  protected void checkNotClosed() {
    checkState(!closed.get(), "Buffer is closed");
  }
}
