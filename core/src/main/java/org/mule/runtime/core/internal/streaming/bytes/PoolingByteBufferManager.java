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
import static java.lang.System.identityHashCode;
import static java.util.Collections.newSetFromMap;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.mule.runtime.api.config.PoolingProfile.DEFAULT_MAX_POOL_WAIT;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_STREAMING_MAX_MEMORY;
import static org.mule.runtime.core.internal.streaming.bytes.ByteStreamingConstants.DEFAULT_BUFFER_BUCKET_SIZE;
import static org.mule.runtime.core.internal.util.ConcurrencyUtils.withLock;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.streaming.bytes.ByteBufferManager;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.internal.streaming.DefaultMemoryManager;
import org.mule.runtime.core.internal.streaming.MemoryManager;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.apache.commons.pool2.KeyedObjectPool;
import org.slf4j.Logger;
import org.vibur.objectpool.ConcurrentPool;
import org.vibur.objectpool.PoolObjectFactory;
import org.vibur.objectpool.PoolService;
import org.vibur.objectpool.util.MultithreadConcurrentQueueCollection;

/**
 * {@link ByteBufferManager} implementation which pools instances for better performance.
 * <p>
 * Buffers are kept in separate pools depending on their capacity.
 * <p>
 * Idle buffers and capacity pools are automatically expired.
 * <p>
 * Unlike traditional pools which are exhausted in terms of number of instances, we don't care about
 * the number of buffers pooled but in the amount of memory they retain. This pool will be exhausted
 * when a certain threshold of retained memory is reached. When exhausted, invokations to
 * {@link #allocate(int)} will block until more memory becomes available (by invoking {@link #deallocate(ByteBuffer)}).
 * If {@link #allocate(int)} is blocked by more than {@link #waitTimeoutMillis} milliseconds, then a
 * {@link MaxStreamingMemoryExceededException} is thrown.
 *
 * @since 4.0
 */
public class PoolingByteBufferManager implements ByteBufferManager, Disposable {

  private static final Logger LOGGER = getLogger(PoolingByteBufferManager.class);
  private static final int DEFAULT_CORE_SIZE = 100;
  static final double MAX_STREAMING_PERCENTILE = 0.7;

  private final AtomicLong streamingMemory = new AtomicLong(0);
  private final int size;
  private final long maxStreamingMemory;
  private final long waitTimeoutMillis;

  private BufferPool defaultSizePool;

  /**
   * Using a cache of pools instead of a {@link KeyedObjectPool} because performance tests indicates that this
   * option is slightly faster, plus it gives us the ability to expire unfrequent capacity buffers without the use
   * of a reaper thread (those performance test did not include such a reaper, so it's very possible that this is more
   * than just slightly faster)
   */
  private final LoadingCache<Integer, BufferPool> customSizePools = Caffeine.newBuilder()
      .expireAfterAccess(5, MINUTES)
      .removalListener((RemovalListener<Integer, BufferPool>) (key, value, cause) -> {
        try {
          value.close();
        } catch (Exception e) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Found exception trying to dispose buffer pool for capacity " + key, e);
          }
        }
      }).build(capacity -> newBufferPool(capacity));

  /**
   * Creates a new instance which allows the pool to grow up to 70% of the runtime's max memory and has a wait timeout of 10
   * seconds. The definition of max memory is that of {@link MemoryManager#getMaxMemory()}
   */
  public PoolingByteBufferManager() {
    this(new DefaultMemoryManager(), DEFAULT_CORE_SIZE, DEFAULT_BUFFER_BUCKET_SIZE, DEFAULT_MAX_POOL_WAIT);
  }

  /**
   * Creates a new instance which allows the pool to grow up to 50% of calling {@link MemoryManager#getMaxMemory()} on the given
   * {@code memoryManager}, and has {@code waitTimeoutMillis} as wait timeout.
   *
   * @param memoryManager     a {@link MemoryManager} used to determine the runtime's max memory
   * @param waitTimeoutMillis how long to wait when the pool is exhausted
   */
  public PoolingByteBufferManager(MemoryManager memoryManager, int size, int bufferSize, long waitTimeoutMillis) {
    maxStreamingMemory = calculateMaxStreamingMemory(memoryManager);
    this.waitTimeoutMillis = waitTimeoutMillis;
    this.size = size;
    defaultSizePool = newBufferPool(bufferSize);
  }

  private long calculateMaxStreamingMemory(MemoryManager memoryManager) {
    String maxMemoryProperty = getProperty(MULE_STREAMING_MAX_MEMORY);
    if (maxMemoryProperty == null) {
      return round(memoryManager.getMaxMemory() * MAX_STREAMING_PERCENTILE);
    } else {
      try {
        return Long.valueOf(maxMemoryProperty);
      } catch (Exception e) {
        throw new IllegalArgumentException(format("Invalid value for system property '%s'. A memory size (in bytes) was "
            + "expected, got '%s' instead", MULE_STREAMING_MAX_MEMORY, maxMemoryProperty));
      }
    }
  }

  private BufferPool newBufferPool(Integer capacity) {
    return new BufferPool(size, capacity);
  }

  private BufferPool getBufferPool(int capacity) {
    return capacity == DEFAULT_BUFFER_BUCKET_SIZE ? defaultSizePool : customSizePools.get(capacity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ByteBuffer allocate(int capacity) {
    try {
      return getBufferPool(capacity).take();
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not allocate byte buffer. " + e.getMessage()), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deallocate(ByteBuffer byteBuffer) {
    int capacity = byteBuffer.capacity();
    BufferPool pool = getBufferPool(capacity);
    if (pool != null) {
      try {
        pool.returnBuffer(byteBuffer);
      } catch (Exception e) {
        throw new MuleRuntimeException(createStaticMessage("Could not deallocate buffer of capacity " + capacity), e);
      }
    }
  }

  @Override
  public void dispose() {
    try {
      defaultSizePool.close();
    } catch (Exception e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Error disposing default capacity byte buffers pool", e);
      }
    }
    try {
      customSizePools.invalidateAll();
    } catch (Exception e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Error disposing mixed capacity byte buffers pool", e);
      }
    }
  }

  private class BufferPool {

    private final int bufferCapacity;
    private final PoolService<ByteBuffer> pool;
    private final Lock lock = new ReentrantLock();
    private final Condition poolNotFull = lock.newCondition();
    private final PoolObjectFactory<ByteBuffer> factory;
    private final Set<Integer> ephemeralBufferIds = newSetFromMap(new ConcurrentHashMap<>());

    private BufferPool(int coreSize, int bufferCapacity) {
      this.bufferCapacity = bufferCapacity;

      factory = new PoolObjectFactory<ByteBuffer>() {

        @Override
        public ByteBuffer create() {
          if (streamingMemory.addAndGet(bufferCapacity) <= maxStreamingMemory) {
            return ByteBuffer.allocate(bufferCapacity);
          }

          streamingMemory.addAndGet(-bufferCapacity);
          throw new MaxStreamingMemoryExceededException(createStaticMessage(format(
                                                                                   "Max streaming memory limit of %d bytes was exceeded",
                                                                                   maxStreamingMemory)));
        }

        @Override
        public boolean readyToTake(ByteBuffer buffer) {
          return true;
        }

        @Override
        public boolean readyToRestore(ByteBuffer buffer) {
          buffer.clear();
          return true;
        }

        @Override
        public void destroy(ByteBuffer buffer) {
          if (streamingMemory.addAndGet(-bufferCapacity) < maxStreamingMemory) {
            signalPoolNotFull();
          }
        }
      };

      //TODO: initial size must be variable
      pool = new ConcurrentPool<>(new MultithreadConcurrentQueueCollection<>(coreSize),
                                  factory, coreSize, coreSize, false);
    }

    private ByteBuffer take() {
      ByteBuffer buffer = null;
      try {
        buffer = pool.tryTake();
        if (buffer == null) {
          buffer = factory.create();
          ephemeralBufferIds.add(identityHashCode(buffer));
        }
      } catch (MaxStreamingMemoryExceededException e) {
        signal(() -> {
          while (streamingMemory.get() >= maxStreamingMemory) {
            if (!poolNotFull.await(waitTimeoutMillis, MILLISECONDS)) {
              throw e;
            }
          }
        });
      }

      return buffer;
    }

    private void returnBuffer(ByteBuffer buffer) {
      try {
        if (ephemeralBufferIds.remove(identityHashCode(buffer))) {
          factory.destroy(buffer);
        } else {
          pool.restore(buffer);
        }
      } finally {
        signalPoolNotFull();
      }
    }

    private void signalPoolNotFull() {
      signal(poolNotFull::signal);
    }

    private void close() {
      streamingMemory.addAndGet(-bufferCapacity * pool.createdTotal());
      try {
        pool.close();
      } finally {
        signal(poolNotFull::signalAll);
      }
    }

    private void signal(CheckedRunnable task) {
      withLock(lock, task);
    }
  }
}
