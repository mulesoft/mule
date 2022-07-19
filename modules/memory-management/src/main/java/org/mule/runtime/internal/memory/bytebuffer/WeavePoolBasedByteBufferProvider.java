/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.internal.memory.bytebuffer;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.MEMORY_BYTE_BUFFER_ALLOCATION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.MEMORY_BYTE_BUFFER_DEALLOCATION;

import static java.lang.System.currentTimeMillis;

import org.mule.runtime.api.memory.provider.ByteBufferProvider;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.type.context.ByteBufferProviderEventContext;
import org.mule.runtime.internal.memory.bytebuffer.profiling.ContainerProfilingScope;
import org.mule.runtime.internal.memory.bytebuffer.profiling.DefaultByteBufferProviderEventContext;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link org.mule.runtime.api.memory.provider.ByteBufferProvider} implementation that can be used to retrieve direct or heap
 * {@link ByteBuffer}'s based on a pool that saves released DirectByteBuffers to be reused.
 *
 * Based on DataWeave Implementation.
 *
 * @since 4.5.0
 */
public class WeavePoolBasedByteBufferProvider implements ByteBufferProvider<ByteBuffer> {

  private final int capacity;
  private final String name;

  private final WeaveByteBufferPool pool;
  ProfilingDataProducer<ByteBufferProviderEventContext, Object> allocationDataProducer;
  ProfilingDataProducer<ByteBufferProviderEventContext, Object> deallocationDataProducer;

  protected WeavePoolBasedByteBufferProvider(String name, int capacity, int maxSize, ProfilingService profilingService) {
    this.name = name;
    this.capacity = capacity;
    this.pool = new WeaveByteBufferPool(capacity, maxSize);
    allocationDataProducer =
        profilingService.getProfilingDataProducer(MEMORY_BYTE_BUFFER_ALLOCATION, new ContainerProfilingScope());
    deallocationDataProducer =
        profilingService.getProfilingDataProducer(MEMORY_BYTE_BUFFER_DEALLOCATION, new ContainerProfilingScope());
  }

  @Override
  public ByteBuffer allocate(int size) {
    allocationDataProducer.triggerProfilingEvent(new DefaultByteBufferProviderEventContext(name, currentTimeMillis(), capacity));
    ByteBuffer byteBuffer = pool.take();
    if (byteBuffer == null) {
      // Takes heap memory
      return ByteBuffer.allocate(capacity);
    } else {
      // Takes direct memory
      return byteBuffer;
    }
  }

  @Override
  public void release(ByteBuffer buffer) {
    deallocationDataProducer
        .triggerProfilingEvent(new DefaultByteBufferProviderEventContext(name, currentTimeMillis(), buffer.limit()));
    if (buffer.isDirect() && buffer.capacity() == capacity) {
      // Release direct memory.
      pool.release(buffer);
    } else {
      // Release heap memory.
    }
  }

  @Override
  public byte[] getByteArray(int size) {
    return new byte[size];
  }

  /**
   * Not used by DataWeave.
   */
  @Override
  public ByteBuffer allocateAtLeast(int size) {
    return null;
  }

  /**
   * Not used by DataWeave.
   */
  @Override
  public ByteBuffer reallocate(ByteBuffer oldBuffer, int newSize) {
    return null;
  }

  @Override
  public void dispose() {
    pool.dispose();
  }


  /**
   * BytBufferPool that saves released DirectByteBuffers to been reused.
   */
  private static final class WeaveByteBufferPool {

    private final int capacity;
    private final int maxSize;

    final private ConcurrentLinkedDeque<ByteBuffer> queue = new ConcurrentLinkedDeque<>();
    private final AtomicInteger amount = new AtomicInteger();

    /**
     * @param capacity The capacity of each BytBuffer
     * @param maxSize  The max amount of elements in the Pool
     */
    private WeaveByteBufferPool(int capacity, int maxSize) {
      this.capacity = capacity;
      this.maxSize = maxSize;
    }

    /**
     * @return A ByteBuffer of the pool previously released or a new one if there is still space in the pool.
     */
    private ByteBuffer take() {
      ByteBuffer buffer = queuePoll();
      if (buffer == null) {
        if (amount.incrementAndGet() <= maxSize) {
          return ByteBuffer.allocateDirect(capacity);
        } else {
          // We have incremented but max size has been exceeded.
          amount.decrementAndGet();
          return null;
        }
      } else {
        // Reset the limit to the capacity.
        buffer.limit(capacity);
        return buffer;
      }
    }

    /**
     * Adds the buffer in the pool to be reused.
     *
     * @param buffer The buffer to be released.
     */
    private void release(ByteBuffer buffer) {
      queueOffer(buffer);
    }

    private int size() {
      return queue.size();
    }

    private ByteBuffer queuePoll() {
      return queue.poll();
    }

    private void queueOffer(ByteBuffer buffer) {
      clear(buffer);
      queue.offerFirst(buffer);
    }

    /**
     * Clear the buffer to be empty in flush mode.
     *
     * @param buffer The buffer to clear.
     */
    private void clear(ByteBuffer buffer) {
      buffer.clear();
      // Put the limit on 0 so no one can write on it.
      buffer.limit(0);
    }

    private Boolean isEmpty() {
      return queue.isEmpty();
    }

    public void dispose() {
      queue.clear();
    }

  }
}

