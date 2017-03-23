/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object;

import org.mule.runtime.api.streaming.Sized;

import java.util.NoSuchElementException;

/**
 * A buffer which provides concurrent random access to the entirety
 * of a dataset.
 * <p>
 * It works with the concept of a zero-base position. Each position
 * represents one byte in the stream. Although this buffer tracks the
 * position of each byte, it doesn't have a position itself. That means
 * that pulling data from this buffer does not make any current position
 * to be moved.
 *
 * @since 4.0
 */
interface ObjectStreamBuffer<T> extends Sized {

  /**
   * Returns the item at the given {@code position}
   * @param position the item's position
   * @return the item
   * @throws NoSuchElementException if there's no item at that position
   */
  T get(long position);

  /**
   * @param position a position in the stream
   * @return Whether there's an item at the given position or not
   */
  boolean hasNext(long position);

  /**
   * Releases all the resources held by this buffer.
   */
  void close();
}
