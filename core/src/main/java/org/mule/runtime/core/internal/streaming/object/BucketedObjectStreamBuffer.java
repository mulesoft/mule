/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import static java.lang.Math.floor;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.util.ConcurrencyUtils.safeUnlock;
import org.mule.runtime.api.streaming.exception.StreamingBufferSizeExceededException;
import org.mule.runtime.core.internal.streaming.object.iterator.StreamingIterator;
import org.mule.runtime.core.streaming.objects.InMemoryCursorIteratorConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * An {@link AbstractObjectStreamBuffer} implementation which uses buckets for locating items.
 * <p>
 * Because this buffer needs to provide random access, array based lists are optimal for obtaining
 * the item in a particular position. However, expanding the capacity of an array based list is very expensive.
 * <p>
 * This buffer works by partitioning the items into buckets of array based lists, so that we never need to expand
 * a list, we simply add a new bucket.
 *
 * @param <T> The generic type of the items in the stream
 * @sice 4.0
 */
public class BucketedObjectStreamBuffer<T> extends AbstractObjectStreamBuffer<T> {

  private final StreamingIterator<T> stream;
  private final InMemoryCursorIteratorConfig config;

  private List<Bucket<T>> buckets;
  private Bucket<T> currentBucket;
  private Position currentPosition;
  private Position maxPosition = null;
  private int instancesCount = 0;

  public BucketedObjectStreamBuffer(StreamingIterator<T> stream, InMemoryCursorIteratorConfig config) {
    this.stream = stream;
    this.config = config;
    initialiseBuckets();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected T doGet(long i) {
    Position position = toPosition(i);
    if (maxPosition != null && maxPosition.compareTo(position) < 0) {
      throw new NoSuchElementException();
    }

    readLock.lock();
    try {
      return getPresentItem(position).orElseGet(() -> {
        safeUnlock(readLock);
        return fetch(position);
      });
    } finally {
      safeUnlock(readLock);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean doHasNext(long i) {
    Position position = toPosition(i);

    readLock.lock();
    try {
      if (maxPosition != null) {
        return position.compareTo(maxPosition) < 1;
      }

      if (position.compareTo(currentPosition) < 1) {
        return true;
      }

      try {
        safeUnlock(readLock);
        fetch(position);
        return true;
      } catch (NoSuchElementException e) {
        return false;
      }
    } finally {
      safeUnlock(readLock);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doClose() {
    closeSafely(stream::close);
    buckets.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return stream.size();
  }

  private void initialiseBuckets() {
    int size = stream.size();
    if (size > 0) {
      maxPosition = toPosition(size - 1);
      buckets = new ArrayList<>(maxPosition.bucketIndex + 1);
    } else {
      buckets = new ArrayList<>();
    }

    currentBucket = new Bucket<>(config.getInitialBufferSize());
    buckets.add(currentBucket);
    currentPosition = new Position(0, -1);
  }

  private Optional<T> getPresentItem(Position position) {
    if (position.bucketIndex < buckets.size()) {
      Bucket<T> bucket = buckets.get(position.bucketIndex);
      return bucket.get(position.itemIndex);
    }

    return empty();
  }

  private T fetch(Position position) {
    writeLock.lock();

    try {
      return getPresentItem(position).orElseGet(() -> {
        T item = null;

        while (currentPosition.compareTo(position) < 0) {
          if (!stream.hasNext()) {
            maxPosition = currentPosition;
            throw new NoSuchElementException();
          }

          item = stream.next();
          if (currentBucket.add(item)) {
            currentPosition = currentPosition.advanceItem();
          } else {
            currentBucket = Bucket.of(item, config.getBufferSizeIncrement());
            buckets.add(currentBucket);
            currentPosition = currentPosition.advanceBucket();
          }
          instancesCount++;
          validateMaxBufferSizeNotExceeded();
        }

        return item;
      });
    } finally {
      writeLock.unlock();
    }
  }

  private void validateMaxBufferSizeNotExceeded() {
    if (instancesCount > config.getMaxInMemoryInstances()) {
      throw new StreamingBufferSizeExceededException(config.getMaxInMemoryInstances());
    }
  }

  private Position toPosition(long position) {
    int initialBufferSize = config.getInitialBufferSize();
    int bucketsDelta = config.getBufferSizeIncrement();

    if (position < initialBufferSize || bucketsDelta == 0) {
      return new Position(0, (int) position);
    }

    long offset = position - initialBufferSize;

    int bucketIndex = (int) floor(offset / bucketsDelta) + 1;
    int itemIndex = (int) position - (initialBufferSize + ((bucketIndex - 1) * bucketsDelta));

    return new Position(bucketIndex, itemIndex);
  }

  private class Position implements Comparable<Position> {

    private final int bucketIndex;
    private final int itemIndex;

    private Position(int bucketIndex, int itemIndex) {
      this.bucketIndex = bucketIndex;
      this.itemIndex = itemIndex;
    }

    private Position advanceItem() {
      return new Position(bucketIndex, itemIndex + 1);
    }

    private Position advanceBucket() {
      return new Position(bucketIndex + 1, 0);
    }

    @Override
    public int compareTo(Position o) {
      int compare = bucketIndex - o.bucketIndex;
      if (compare == 0) {
        compare = itemIndex - o.itemIndex;
      }

      return compare;
    }
  }


  private static class Bucket<T> {

    private final List<T> items;
    private final int capacity;

    private Bucket(int capacity) {
      this.capacity = capacity;
      this.items = new ArrayList<>(capacity);
    }

    private static <T> Bucket<T> of(T initialItem, int capacity) {
      Bucket<T> bucket = new Bucket<>(capacity);
      bucket.add(initialItem);

      return bucket;
    }

    private boolean add(T item) {
      if (items.size() < capacity) {
        items.add(item);
        return true;
      }

      return false;
    }

    private Optional<T> get(int index) {
      if (index < items.size()) {
        return ofNullable(items.get(index));
      }
      return empty();
    }
  }
}
