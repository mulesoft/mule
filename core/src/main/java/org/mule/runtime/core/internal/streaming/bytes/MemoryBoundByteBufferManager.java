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
import org.mule.runtime.api.util.MuleSystemProperties;
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

/**
 * Base implementation of a {@link ByteBufferManager} which makes sure that the total memory footprints of all
 * the {@link ByteBuffer buffers} obtained through this class do not exceed a maximum cap.
 * <p>
 * Said memory cap can be given through either the {@link MuleSystemProperties#MULE_STREAMING_MAX_MEMORY} or
 * {@link MuleSystemProperties#MULE_STREAMING_MAX_MEMORY_PERCENTAGE} system properties. If none is set, the cap will
 * default to 70% of the total heap.
 * <p>
 * Implementations of this class <b>MUST</b> always implement their allocation logic through the {@link #allocateIfFits(int)}
 * method.
 * <p>
 * If the memory cap is exceeded, a {@link MaxStreamingMemoryExceededException} is thrown.
 *
 * @since 4.3.0
 */
public abstract class MemoryBoundByteBufferManager implements ByteBufferManager, Disposable {

  private static final Logger LOGGER = getLogger(MemoryBoundByteBufferManager.class);

  protected static final long DEFAULT_MEMORY_EXHAUSTED_WAIT_TIME = 1000;

  private final AtomicLong streamingMemory = new AtomicLong(0);
  private final long maxStreamingMemory;
  private final long memoryExhaustedWaitTimeoutMillis;
  private final Lock lock = new ReentrantLock();
  private final Condition poolNotFull = lock.newCondition();

  /**
   * Creates a new instance
   */
  public MemoryBoundByteBufferManager() {
    this(new DefaultMemoryManager(), DEFAULT_MEMORY_EXHAUSTED_WAIT_TIME);
  }

  /**
   * Creates a new instance
   *
   * @param memoryManager                    the {@link MemoryManager}  through which heap status is obtained
   * @param memoryExhaustedWaitTimeoutMillis how much time to wait for more memory to become available before throwing exceptions
   */
  public MemoryBoundByteBufferManager(MemoryManager memoryManager, long memoryExhaustedWaitTimeoutMillis) {
    maxStreamingMemory = calculateMaxStreamingMemory(memoryManager);
    this.memoryExhaustedWaitTimeoutMillis = memoryExhaustedWaitTimeoutMillis;
  }

  /**
   * Tries to allocate a {@link ByteBuffer} of the given {@code capacity}. If said operation exceeds the memory cap, then a
   * {@link MaxStreamingMemoryExceededException} is thrown
   *
   * @param capacity the required buffer's capacity
   * @return a {@link ByteBuffer}
   * @throws MaxStreamingMemoryExceededException if the memory cap is exceeded by this operation
   */
  protected final ByteBuffer allocateIfFits(int capacity) {
    if (streamingMemory.addAndGet(capacity) <= maxStreamingMemory) {
      return ByteBuffer.allocate(capacity);
    }

    streamingMemory.addAndGet(-capacity);
    throw new MaxStreamingMemoryExceededException(createStaticMessage(format(
                                                                             "Max streaming memory limit of %d bytes was exceeded",
                                                                             maxStreamingMemory)));
  }

  /**
   * Tries to allocate the {@link ByteBuffer} by delegating to {@link #doAllocate(int)}. If the memory cap is exceeded, it
   * will wait for up to {@link #memoryExhaustedWaitTimeoutMillis} for more memory to be available. If the timeout runs out,
   * then the exception is thrown.
   * <p>
   * This method is final, actual allocation logic <b>MUST</b> happen on the {@link #doAllocate(int)} method, which in turn
   * <b>MUST</b> also delegate into {@link #allocateIfFits(int)}
   *
   * @param capacity the capacity of the returned buffer
   * @return a {@link ByteBuffer}
   * @throws MaxStreamingMemoryExceededException if the memory cap is exceeded.
   */
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
      signalMemoryAvailable();
    }
  }

  @Override
  public void dispose() {
    signal(poolNotFull::signalAll);
  }

  /**
   * This method is to be invoked when the memory cap is exceeded. This method will blcok for up to {@code waitTimeoutMillis}
   * waiting for more memory to be available.
   * <p>
   * If the timeout runs out, then the {@code e} is thrown.
   *
   * @param waitTimeoutMillis the max wait timeout
   * @param exceeded          the exception to throw if the timeout is exceeded.
   * @throws InterruptedException
   */
  protected void awaitNotFull(long waitTimeoutMillis, MaxStreamingMemoryExceededException exceeded) throws InterruptedException {
    if (!poolNotFull.await(waitTimeoutMillis, MILLISECONDS)) {
      LOGGER.debug("Couldn't reclaim enough streaming memory after {} milliseconds. Will fail", memoryExhaustedWaitTimeoutMillis);
      throw exceeded;
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

  /**
   * Signals to one waiting threads that streaming memory is available
   */
  protected void signalMemoryAvailable() {
    signal(poolNotFull::signal);
  }

  /**
   * Executes a task meant to signal waiting threads about updated memory conditions
   *
   * @param task a task
   */
  protected void signal(CheckedRunnable task) {
    withLock(lock, task);
  }
}
