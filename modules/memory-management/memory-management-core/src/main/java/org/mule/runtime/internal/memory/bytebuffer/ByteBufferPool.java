/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.internal.memory.bytebuffer;

import java.nio.ByteBuffer;

/**
 * A thread local pool used by a {@link org.mule.runtime.api.memory.provider.ByteBufferProvider} to create and modify Buffers
 *
 * @param <T> Type of Buffer that will be created
 *
 *            Based on Grizzly Implementation
 *
 * @since 4.5.0
 */
public interface ByteBufferPool<T extends ByteBuffer> {

  /**
   * Creates a new Buffer with a set size and assigns it the data that was held in the old one as long as the given size is not
   * smaller than the data held.
   *
   * @param oldByteBuffer Old Buffer containing data
   * @param newSize       The size the new Buffer should be.
   * @return the new Buffer or null if the buffer could not be resized
   */
  ByteBuffer reallocate(ByteBuffer oldByteBuffer, int newSize);

  /**
   * deallocates the data in the buffer
   *
   * @param byteBuffer the buffer to release
   * @return true if operation successfully completed, false otherwise
   */
  boolean release(ByteBuffer byteBuffer);

  /**
   * Reduces the buffer to the last data allocated
   *
   * @param byteBuffer
   * @return the old buffer data that was removed. This may be null.
   */
  ByteBuffer reduceLastAllocated(ByteBuffer byteBuffer);

  /**
   * Gets the number of elements between the current position and the limit
   *
   * @return number of elements
   */
  int remaining();

  /**
   * Resets the Buffer to empty values and empties the pool
   *
   * @param byteBuffer the buffer to reset
   */
  void reset(T byteBuffer);

  /**
   * Creates a buffer with a given capacity and limit
   *
   * @param size maximum number of elements
   * @return the new buffer
   * @see java.nio.ByteBuffer#allocate(int)
   */
  T allocate(int size);

  /**
   * Whether there are elements between the current position and the end
   *
   * @return true if there are unused elements, false otherwise
   */
  boolean hasRemaining();

  /**
   * The Max Buffer Size for buffer
   *
   * @return the max buffer size.
   */
  int getMaxBufferSize();

  /**
   * dospose the thread local pool.
   */
  void dispose();

}
