/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming.bytes;

import org.mule.runtime.core.internal.streaming.bytes.MaxStreamingMemoryExceededException;

import java.nio.ByteBuffer;

/**
 * Manages the lifecycle of {@link ByteBuffer} instances, so that Mule can keep track
 * of how much buffer memory is being consumed by the owner of this manager.
 * <p>
 * Even though the {@link ByteBuffer} API doesn't have the concept of deallocation, for the purposes
 * of this class every buffer obtained through the {@link #allocate(int)} method should eventually be
 * passed to the {@link #deallocate(ByteBuffer)} method once the instance is no longer needed.
 *
 * @since 4.0
 */
public interface ByteBufferManager {

  /**
   * Returns a {@link ByteBuffer} of the given {@code capacity}.
   * <p>
   * Invokers <b>MUST</b> call the {@link #deallocate(ByteBuffer)} method with the returned
   * buffer once it's no longer needed.
   *
   * @param capacity the capacity of the returned buffer
   * @return a {@link ByteBuffer} of the given {@code capacity}
   * @throws MaxStreamingMemoryExceededException if no more streaming memory is available
   */
  ByteBuffer allocate(int capacity);

  /**
   * Indicates that the given {@code byteBuffer} is no longer needed and the runtime
   * may dispose of it however it sees fit.
   *
   * @param byteBuffer the buffer to be deallocated.
   */
  void deallocate(ByteBuffer byteBuffer);
}
