/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import org.mule.runtime.api.streaming.HasSize;

import java.util.Optional;

/**
 * A buffer which provides concurrent random access to the entirety
 * of a stream of objects.
 * <p>
 * It works with the concept of a zero-base position. Each position
 * represents one byte in the stream. Although this buffer tracks the
 * position of each byte, it doesn't have a position itself. That means
 * that pulling data from this buffer does not make any current position
 * to be moved.
 *
 * It uses the concept of {@link Bucket} to store and return items. Because this buffer needs to provide
 * random access, array based lists are optimal for obtaining the item in a particular position.
 * However, expanding the capacity of an array based list is very expensive.
 * <p>
 * This buffer works by partitioning the items into buckets of array based lists, so that we never need to expand
 * a list, we simply add a new bucket.
 *
 * @since 4.0
 */
public interface ObjectStreamBuffer<T> extends HasSize {

  /**
   * Returns the {@link Bucket} for the given {@code position}
   *
   * @param position the bucket's position
   * @return an optional the bucket. Will be empty if no bucket for that position,
   */
  Optional<Bucket<T>> getBucketFor(Position position);

  /**
   * Transforms the given index based {@code position} to a {@link Position}
   * object
   *
   * @param position a zero based index position
   * @return a {@link Position}
   */
  Position toPosition(long position);

  /**
   * @param position a position in the stream
   * @return Whether there's an item at the given position or not
   */
  boolean hasNext(long position);

  /**
   * Initialises this buffer. Should be invoked on every instance in order to
   * make it functional
   */
  void initialise();

  /**
   * Releases all the resources held by this buffer.
   */
  void close();
}
