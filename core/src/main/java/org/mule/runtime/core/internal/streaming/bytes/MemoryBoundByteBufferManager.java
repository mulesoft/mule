/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.lang.Math.round;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_STREAMING_MAX_MEMORY;
import static org.mule.runtime.core.internal.streaming.bytes.ByteStreamingConstants.MAX_STREAMING_MEMORY_PERCENTAGE;

import org.mule.runtime.api.util.MuleSystemProperties;
import org.mule.runtime.core.api.streaming.bytes.ByteBufferManager;
import org.mule.runtime.core.api.streaming.bytes.ManagedByteBufferWrapper;
import org.mule.runtime.core.internal.streaming.DefaultMemoryManager;
import org.mule.runtime.core.internal.streaming.MemoryManager;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Base implementation of a {@link ByteBufferManager} which makes sure that the total memory footprints of all
 * the {@link ByteBuffer buffers} obtained through this class do not exceed a maximum cap.
 * <p>
 * Said memory cap can be given through either the {@link MuleSystemProperties#MULE_STREAMING_MAX_MEMORY} or
 * {@link MuleSystemProperties#MULE_STREAMING_MAX_HEAP_PERCENTAGE} system properties. If none is set, the cap will
 * default to 70% of the total heap.
 * <p>
 * Implementations of this class <b>MUST</b> always implement their allocation logic through the {@link #allocateIfFits(int)}
 * method.
 * <p>
 * If the memory cap is exceeded, a {@link MaxStreamingMemoryExceededException} is thrown.
 *
 * @since 4.3.0
 */
public abstract class MemoryBoundByteBufferManager implements ByteBufferManager {

  private final AtomicLong streamingMemory = new AtomicLong(0);
  private final long maxStreamingMemory;

  /**
   * Creates a new instance
   */
  public MemoryBoundByteBufferManager() {
    this(new DefaultMemoryManager());
  }

  /**
   * Creates a new instance
   *
   * @param memoryManager the {@link MemoryManager}  through which heap status is obtained
   */
  public MemoryBoundByteBufferManager(MemoryManager memoryManager) {
    maxStreamingMemory = calculateMaxStreamingMemory(memoryManager);
  }

  /**
   * Tries to allocate a {@link ManagedByteBufferWrapper} of the given {@code capacity}.
   * <p>
   * If said operation exceeds the memory cap, then a {@link MaxStreamingMemoryExceededException} is thrown.
   *
   * @param capacity the required buffer's capacity
   * @return a {@link ManagedByteBufferWrapper}
   * @throws MaxStreamingMemoryExceededException if the memory cap is exceeded by this operation
   */
  protected final ByteBuffer allocateIfFits(int capacity) {
    if (streamingMemory.addAndGet(capacity) <= maxStreamingMemory) {
      return ByteBuffer.allocate(capacity);
    }

    streamingMemory.addAndGet(-capacity);
    throw new MaxStreamingMemoryExceededException(createStaticMessage(
                                                                      format("Max streaming memory limit of %d bytes was exceeded",
                                                                             maxStreamingMemory)));
  }

  /**
   * Tries to allocate the {@link ByteBuffer} by delegating to {@link #doAllocate(int)}. If the memory cap is exceeded
   * a {@link MaxStreamingMemoryExceededException} is thrown.
   * <p>
   * This method is final, actual allocation logic <b>MUST</b> happen on the {@link #doAllocate(int)} method, which in turn
   * <b>MUST</b> also delegate into {@link #allocateIfFits(int)}
   *
   * @param capacity the capacity of the returned buffer
   * @return a {@link ByteBuffer}
   * @throws MaxStreamingMemoryExceededException if the memory cap is exceeded.
   */
  @Override
  @Deprecated
  public ByteBuffer allocate(int capacity) {
    return allocateIfFits(capacity);
  }

  @Override
  @Deprecated
  public void deallocate(ByteBuffer byteBuffer) {
    doDeallocate(byteBuffer);
  }

  protected void doDeallocate(ByteBuffer byteBuffer) {
    streamingMemory.addAndGet(-byteBuffer.capacity());
  }

  private long calculateMaxStreamingMemory(MemoryManager memoryManager) {
    String maxMemoryProperty = getProperty(MULE_STREAMING_MAX_MEMORY);
    if (maxMemoryProperty == null) {
      return round(memoryManager.getMaxMemory() * MAX_STREAMING_MEMORY_PERCENTAGE);
    } else {
      try {
        return Long.valueOf(maxMemoryProperty);
      } catch (Exception e) {
        throw new IllegalArgumentException(format("Invalid value for system property '%s'. A memory size (in bytes) was "
            + "expected, got '%s' instead", MULE_STREAMING_MAX_MEMORY,
                                                  maxMemoryProperty));
      }
    }
  }
}
