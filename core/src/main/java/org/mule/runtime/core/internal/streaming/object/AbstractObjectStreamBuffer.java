/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_OBJECT_STREAMING_BUFFER_SIZE;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.streaming.HasSize;
import org.mule.runtime.core.internal.streaming.AbstractStreamingBuffer;

import java.io.Closeable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;

/**
 * Base class for implementations of {@link ObjectStreamBuffer}
 *
 * @param <T> the generic type of the items in the stream
 * @since 4.0
 */
public abstract class AbstractObjectStreamBuffer<T> extends AbstractStreamingBuffer implements ObjectStreamBuffer<T> {

  private static final Logger LOGGER = getLogger(AbstractObjectStreamBuffer.class);

  private final Iterator<T> stream;
  private final Supplier<Integer> sizeResolver;

  private Bucket<T> currentBucket = new Bucket<>(0, DEFAULT_OBJECT_STREAMING_BUFFER_SIZE);
  private Position currentPosition;
  private Position maxPosition = null;
  private int instancesCount = 0;

  public AbstractObjectStreamBuffer(Iterator<T> stream) {
    this.stream = stream;
    sizeResolver = stream instanceof HasSize ? () -> ((HasSize) stream).getSize() : () -> -1;
  }

  @Override
  public final void initialise() {
    currentPosition = new Position(0, -1);
    int size = getSize();
    if (size > 0) {
      setMaxPosition(toPosition(size - 1));
    }
    initialize(ofNullable(maxPosition), currentBucket);
  }

  protected abstract void initialize(Optional<Position> maxPosition, Bucket<T> initialBucket);

  @Override
  public Bucket<T> getBucketFor(Position position) {
    checkNotClosed();

    if (maxPosition != null && maxPosition.compareTo(position) < 0) {
      throw new NoSuchElementException();
    }

    readLock.lock();
    try {
      Bucket<T> bucket = getPresentBucket(position);
      if (bucket != null) {
        return forwarding(bucket);
      }
    } finally {
      readLock.unlock();
    }

    return fetch(position);
  }

  @Override
  public int getSize() {
    return sizeResolver.get();
  }

  @Override
  public final boolean hasNext(long i) {
    if (closed.get()) {
      return false;
    }

    Position position = toPosition(i);
    boolean readLockAcquired = true;
    readLock.lock();
    try {
      if (maxPosition != null) {
        return position.compareTo(maxPosition) < 1;
      }

      if (position.compareTo(currentPosition) < 1) {
        return true;
      }

      readLockAcquired = false;
      readLock.unlock();

      try {
        return fetch(position) != null;
      } catch (NoSuchElementException e) {
        return false;
      }
    } finally {
      if (readLockAcquired) {
        readLock.unlock();
      }
    }
  }

  private Bucket<T> fetch(Position position) {
    writeLock.lock();
    try {
      Bucket<T> presentBucket = getPresentBucket(position);
      if (presentBucket != null && presentBucket.contains(position)) {
        return presentBucket;
      }

      while (currentPosition.compareTo(position) < 0) {
        if (!stream.hasNext()) {
          maxPosition = currentPosition;
          return null;
        }

        T item = stream.next();
        if (currentBucket.add(item)) {
          currentPosition = currentPosition.advanceItem();
        } else {
          setCurrentBucket(onBucketOverflow(currentBucket));
          currentBucket.add(item);

          currentPosition = currentPosition.advanceBucket();
        }
        instancesCount++;
        validateMaxBufferSizeNotExceeded(instancesCount);
      }

      return currentBucket;
    } finally {
      writeLock.unlock();
    }
  }

  protected abstract void validateMaxBufferSizeNotExceeded(int instancesCount);

  protected abstract Bucket<T> onBucketOverflow(Bucket<T> overflownBucket);

  @Override
  public final void close() {
    if (closed.compareAndSet(false, true)) {
      writeLock.lock();
      try {
        doClose();
      } finally {
        if (stream instanceof Closeable) {
          try {
            ((Closeable) stream).close();
          } catch (Exception e) {
            LOGGER.debug("Found exception trying to close Object stream", e);
          }
        }
        setCurrentBucket(null);
        writeLock.unlock();
      }
    }
  }

  protected abstract void doClose();

  protected abstract Bucket<T> getPresentBucket(Position position);

  private Bucket<T> forwarding(Bucket<T> presentBucket) {
    if (presentBucket != null) {
      return new ForwardingBucket<>(presentBucket);
    }

    return null;
  }

  protected Bucket<T> getCurrentBucket() {
    return currentBucket;
  }

  protected void setCurrentBucket(Bucket<T> bucket) {
    currentBucket = bucket;
  }

  protected void setMaxPosition(Position maxPosition) {
    this.maxPosition = maxPosition;
  }

  private class ForwardingBucket<T> extends Bucket<T> {

    private Bucket<T> delegate;

    private ForwardingBucket(Bucket<T> delegate) {
      super(delegate.getIndex(), 0);
      this.delegate = delegate;
    }

    @Override
    public Optional<T> get(int index) {
      boolean readLockAcquired = true;
      readLock.lock();
      try {
        Optional<T> item = delegate.get(index);
        if (item.isPresent()) {
          return item;
        }

        Position position = new Position(delegate.getIndex(), index);
        readLockAcquired = false;
        readLock.unlock();
        delegate = (Bucket<T>) fetch(position);

        if (delegate == null) {
          throw new NoSuchElementException();
        }

        readLock.lock();
        readLockAcquired = true;
        return delegate.get(index);
      } finally {
        if (readLockAcquired) {
          readLock.unlock();
        }
      }
    }

    @Override
    public boolean contains(Position position) {
      return delegate.contains(position);
    }

    @Override
    public int getIndex() {
      return delegate.getIndex();
    }
  }
}
