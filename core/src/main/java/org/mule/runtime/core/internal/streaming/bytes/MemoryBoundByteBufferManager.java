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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_STREAMING_MAX_MEMORY;
import static org.mule.runtime.core.internal.streaming.bytes.ByteStreamingConstants.MAX_STREAMING_MEMORY_PERCENTAGE;
import static org.mule.runtime.core.internal.util.ConcurrencyUtils.withLock;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.streaming.bytes.ByteBufferManager;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.internal.streaming.DefaultMemoryManager;
import org.mule.runtime.core.internal.streaming.MemoryManager;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

public abstract class MemoryBoundByteBufferManager implements ByteBufferManager, Disposable {

  private final AtomicLong streamingMemory = new AtomicLong(0);
  private final long maxStreamingMemory;
  private final Lock lock = new ReentrantLock();
  private final Condition poolNotFull = lock.newCondition();

  public MemoryBoundByteBufferManager() {
    this(new DefaultMemoryManager());
  }

  public MemoryBoundByteBufferManager(MemoryManager memoryManager) {
    maxStreamingMemory = calculateMaxStreamingMemory(memoryManager);
  }


  protected ByteBuffer allocateIfFits(Function<Integer, ByteBuffer> factory, int capacity) {
    if (streamingMemory.addAndGet(capacity) <= maxStreamingMemory) {
      return factory.apply(capacity);
    }

    streamingMemory.addAndGet(-capacity);
    throw new MaxStreamingMemoryExceededException(createStaticMessage(format(
                                                                             "Max streaming memory limit of %d bytes was exceeded",
                                                                             maxStreamingMemory)));
  }

  @Override
  public void deallocate(ByteBuffer byteBuffer) {
    if (streamingMemory.addAndGet(-byteBuffer.capacity()) < maxStreamingMemory) {
      signalPoolNotFull();
    }
  }

  @Override
  public void dispose() {
    signal(poolNotFull::signalAll);
  }

  protected void awaitNotFull(long waitTimeoutMillis, MaxStreamingMemoryExceededException e) throws InterruptedException {
    if (!poolNotFull.await(waitTimeoutMillis, MILLISECONDS)) {
      throw e;
    }
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

  protected void signalPoolNotFull() {
    signal(poolNotFull::signal);
  }

  protected void signal(CheckedRunnable task) {
    withLock(lock, task);
  }
}
