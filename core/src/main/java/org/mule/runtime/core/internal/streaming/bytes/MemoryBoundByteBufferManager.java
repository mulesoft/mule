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
import static org.slf4j.LoggerFactory.getLogger;

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

import org.slf4j.Logger;

public abstract class MemoryBoundByteBufferManager implements ByteBufferManager, Disposable {

  private static final Logger LOGGER = getLogger(MemoryBoundByteBufferManager.class);

  protected static final long DEFAULT_MEMORY_EXHAUSTED_WAIT_TIME = 1000;

  private final AtomicLong streamingMemory = new AtomicLong(0);
  private final long maxStreamingMemory;
  private final long memoryExhaustedWaitTimeoutMillis;
  private final Lock lock = new ReentrantLock();
  private final Condition poolNotFull = lock.newCondition();

  public MemoryBoundByteBufferManager() {
    this(new DefaultMemoryManager(), DEFAULT_MEMORY_EXHAUSTED_WAIT_TIME);
  }

  public MemoryBoundByteBufferManager(MemoryManager memoryManager, long memoryExhaustedWaitTimeoutMillis) {
    maxStreamingMemory = calculateMaxStreamingMemory(memoryManager);
    this.memoryExhaustedWaitTimeoutMillis = memoryExhaustedWaitTimeoutMillis;
  }


  protected ByteBuffer allocateIfFits(int capacity) {
    if (streamingMemory.addAndGet(capacity) <= maxStreamingMemory) {
      return ByteBuffer.allocate(capacity);
    }

    streamingMemory.addAndGet(-capacity);
    throw new MaxStreamingMemoryExceededException(createStaticMessage(format(
        "Max streaming memory limit of %d bytes was exceeded",
        maxStreamingMemory)));
  }

  @Override
  public final ByteBuffer allocate(int capacity) {
    ByteBuffer buffer = null;
    boolean isRetry = false;
    do {
      try {
        buffer = doAllocate(capacity);
      } catch (MaxStreamingMemoryExceededException e) {
        if (isRetry) {
          throw e;
        }

        LOGGER.debug("Max streaming memory threshold of {} achieved. Will wait {} ms for reclaimed memory to retry",
                     maxStreamingMemory, memoryExhaustedWaitTimeoutMillis);
        signal(() -> awaitNotFull(memoryExhaustedWaitTimeoutMillis, e));
        isRetry = true;
      }
    } while (buffer == null);

    return buffer;
  }

  protected ByteBuffer doAllocate(int capacity) {
    return allocateIfFits(capacity);
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
      LOGGER.debug("Couldn't reclaim enough streaming memory after {} milliseconds. Will fail", memoryExhaustedWaitTimeoutMillis);
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
