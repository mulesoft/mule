/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import java.nio.ByteBuffer;

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
public interface InputStreamBuffer {

  /**
   * Loads information into the given {@code destination}
   *
   * @param destination the buffer in which the data is to be loaded. The buffer has to be in the correct position
   *                    and have at least {@code length} bytes remaining
   * @param position    the stream position from which the data should be read
   * @param length      how many bytes to read
   * @return how many bytes were actually read, or {@code -1} if no data is available for the given {@code position}
   */
  int get(ByteBuffer destination, long position, int length);

  /**
   * Releases all the resources held by this buffer
   */
  void close();
}
