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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.mule.runtime.api.config.PoolingProfile.DEFAULT_MAX_POOL_WAIT;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_STREAMING_MAX_MEMORY;
import static org.mule.runtime.core.internal.util.ConcurrencyUtils.withLock;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.streaming.bytes.ByteBufferManager;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.internal.streaming.DefaultMemoryManager;
import org.mule.runtime.core.internal.streaming.MemoryManager;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;

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
  private static final int MAX_IDLE = Runtime.getRuntime().availableProcessors();

  private final AtomicLong streamingMemory = new AtomicLong(0);
  private final long maxStreamingMemory;
  private final long waitTimeoutMillis;

  /**
   * Using a cache of pools instead of a {@link KeyedObjectPool} because performance tests indicates that this
   * option is slightly faster, plus it gives us the ability to expire unfrequent capacity buffers without the use
   * of a reaper thread (those performance test did not include such a reaper, so it's very possible that this is more
   * than just slightly faster)
   */
  private final LoadingCache<Integer, BufferPool> pools = CacheBuilder.newBuilder()
      .expireAfterAccess(10, SECONDS)
      .removalListener((RemovalListener<Integer, BufferPool>) notification -> {
        try {
          notification.getValue().close();
        } catch (Exception e) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Found exception trying to dispose buffer pool for capacity " + notification.getKey(), e);
          }
        }
      }).build(new CacheLoader<Integer, BufferPool>() {

        @Override
        public BufferPool load(Integer capacity) throws Exception {
          return new BufferPool(capacity);
        }
      });

  /**
   * Creates a new instance which allows the pool to grow up to 50% of the runtime's max memory and has a wait
   * timeout of 10 seconds. The definition of max memory is that of {@link MemoryManager#getMaxMemory()}
   */
  public PoolingByteBufferManager() {
    this(new DefaultMemoryManager(), DEFAULT_MAX_POOL_WAIT);
  }

  /**
   * Creates a new instance which allows the pool to grow up to 50% of calling {@link MemoryManager#getMaxMemory()}
   * on the given {@code memoryManager}, and has {@code waitTimeoutMillis} as wait timeout.
   *
   * @param memoryManager     a {@link MemoryManager} used to determine the runtime's max memory
   * @param waitTimeoutMillis how long to wait when the pool is exhausted
   */
  public PoolingByteBufferManager(MemoryManager memoryManager, long waitTimeoutMillis) {
    maxStreamingMemory = calculateMaxStreamingMemory(memoryManager);
    this.waitTimeoutMillis = waitTimeoutMillis;
  }

  private long calculateMaxStreamingMemory(MemoryManager memoryManager) {
    String maxMemoryProperty = getProperty(MULE_STREAMING_MAX_MEMORY);
    if (maxMemoryProperty == null) {
      return round(memoryManager.getMaxMemory() * 0.5);
    } else {
      try {
        return Long.valueOf(maxMemoryProperty);
      } catch (Exception e) {
        throw new IllegalArgumentException(format("Invalid value for system property '%s'. A memory size (in bytes) was "
            + "expected, got '%s' instead",
                                                  MULE_STREAMING_MAX_MEMORY, maxMemoryProperty));
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ByteBuffer allocate(int capacity) {
    try {
      return pools.getUnchecked(capacity).take();
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
    BufferPool pool = pools.getIfPresent(capacity);
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
      pools.invalidateAll();
    } catch (Exception e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn("Error disposing pool of byte buffers", e);
      }
    }
  }

  private class BufferPool {

    private final int bufferCapacity;
    private final ObjectPool<ByteBuffer> pool;
    private final Lock lock = new ReentrantLock();
    private final Condition poolNotFull = lock.newCondition();

    private BufferPool(int bufferCapacity) {
      this.bufferCapacity = bufferCapacity;
      GenericObjectPoolConfig config = new GenericObjectPoolConfig();
      config.setMaxIdle(MAX_IDLE);
      config.setMaxTotal(-1);
      config.setBlockWhenExhausted(false);
      config.setTimeBetweenEvictionRunsMillis(SECONDS.toMillis(30));
      config.setTestOnBorrow(false);
      config.setTestOnReturn(false);
      config.setTestWhileIdle(false);
      config.setTestOnCreate(false);
      config.setJmxEnabled(false);

      pool = new GenericObjectPool<>(new BasePooledObjectFactory<ByteBuffer>() {

        @Override
        public ByteBuffer create() throws Exception {
          if (streamingMemory.addAndGet(bufferCapacity) <= maxStreamingMemory) {
            return ByteBuffer.allocate(bufferCapacity);
          }

          streamingMemory.addAndGet(-bufferCapacity);
          throw new MaxStreamingMemoryExceededException(createStaticMessage(format(
                                                                                   "Max streaming memory limit of %d bytes was exceeded",
                                                                                   maxStreamingMemory)));
        }

        @Override
        public PooledObject<ByteBuffer> wrap(ByteBuffer obj) {
          return new DefaultPooledObject<>(obj);
        }

        @Override
        public void activateObject(PooledObject<ByteBuffer> p) throws Exception {
          p.getObject().clear();
        }

        @Override
        public void destroyObject(PooledObject<ByteBuffer> p) throws Exception {
          if (streamingMemory.addAndGet(-bufferCapacity) < maxStreamingMemory) {
            signalPoolNotFull();
          }
        }
      }, config);
    }

    private ByteBuffer take() throws Exception {
      ByteBuffer buffer = null;
      do {
        try {
          buffer = pool.borrowObject();
        } catch (MaxStreamingMemoryExceededException e) {
          signal(() -> {
            while (streamingMemory.get() >= maxStreamingMemory) {
              if (!poolNotFull.await(waitTimeoutMillis, MILLISECONDS)) {
                throw e;
              }
            }
          });
        }
      } while (buffer == null);

      return buffer;
    }

    private void returnBuffer(ByteBuffer buffer) throws Exception {
      pool.returnObject(buffer);
      signalPoolNotFull();
    }

    private void signalPoolNotFull() {
      signal(poolNotFull::signal);
    }

    private void close() {
      streamingMemory.addAndGet(-bufferCapacity * (pool.getNumActive() + pool.getNumIdle()));
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
