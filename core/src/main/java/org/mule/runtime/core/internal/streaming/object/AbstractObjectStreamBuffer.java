/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.extension.api.ExtensionConstants.DEFAULT_OBJECT_STREAMING_BUFFER_SIZE;
import org.mule.runtime.core.internal.streaming.AbstractStreamingBuffer;
import org.mule.runtime.core.api.streaming.iterator.StreamingIterator;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Base class for implementations of {@link ObjectStreamBuffer}
 *
 * @param <T> the generic type of the items in the stream
 * @since 4.0
 */
public abstract class AbstractObjectStreamBuffer<T> extends AbstractStreamingBuffer implements ObjectStreamBuffer<T> {

  private final StreamingIterator<T> stream;

  private Bucket<T> currentBucket = new Bucket<>(0, DEFAULT_OBJECT_STREAMING_BUFFER_SIZE);
  private Position currentPosition;
  private Position maxPosition = null;
  private int instancesCount = 0;

  public AbstractObjectStreamBuffer(StreamingIterator<T> stream) {
    this.stream = stream;
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
  public Optional<Bucket<T>> getBucketFor(Position position) {
    checkNotClosed();

    if (maxPosition != null && maxPosition.compareTo(position) < 0) {
      throw new NoSuchElementException();
    }
    return withReadLock(releaser -> {
      Optional<Bucket<T>> bucket = getPresentBucket(position);
      if (bucket.isPresent()) {
        return forwarding(bucket);
      }

      releaser.release();
      return fetch(position);
    });
  }

  @Override
  public int getSize() {
    return stream.getSize();
  }

  @Override
  public final boolean hasNext(long i) {
    if (closed.get()) {
      return false;
    }

    Position position = toPosition(i);

    return withReadLock(releaser -> {
      if (maxPosition != null) {
        return position.compareTo(maxPosition) < 1;
      }

      if (position.compareTo(currentPosition) < 1) {
        return true;
      }

      releaser.release();
      try {
        return fetch(position).isPresent();
      } catch (NoSuchElementException e) {
        return false;
      }
    });
  }

  private Optional<Bucket<T>> fetch(Position position) {
    return withWriteLock(() -> {
      Optional<Bucket<T>> presentBucket = getPresentBucket(position);
      if (presentBucket.filter(bucket -> bucket.contains(position)).isPresent()) {
        return presentBucket;
      }

      while (currentPosition.compareTo(position) < 0) {
        if (!stream.hasNext()) {
          maxPosition = currentPosition;
          return empty();
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

      return of(currentBucket);
    });
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
        closeSafely(stream::close);
        setCurrentBucket(null);
        writeLock.unlock();
      }
    }
  }

  protected abstract void doClose();

  protected abstract Optional<Bucket<T>> getPresentBucket(Position position);

  private Optional<Bucket<T>> forwarding(Optional<Bucket<T>> presentBucket) {
    return presentBucket.map(b -> new ForwardingBucket<>(b));
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
      return withReadLock(releaser -> {
        Optional<T> item = delegate.get(index);
        if (item.isPresent()) {
          return item;
        }

        Position position = new Position(delegate.getIndex(), index);
        releaser.release();
        delegate = (Bucket<T>) fetch(position).orElseThrow(NoSuchElementException::new);
        return withReadLock(r2 -> delegate.get(index));
      });
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
