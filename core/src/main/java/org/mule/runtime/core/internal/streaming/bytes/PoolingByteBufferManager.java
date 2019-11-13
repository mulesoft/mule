/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.lang.Math.min;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.identityHashCode;
import static java.util.Collections.newSetFromMap;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.internal.streaming.bytes.ByteStreamingConstants.DEFAULT_BUFFER_BUCKET_SIZE;
import static org.mule.runtime.core.internal.streaming.bytes.ByteStreamingConstants.DEFAULT_BUFFER_POOL_SIZE;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.internal.streaming.DefaultMemoryManager;
import org.mule.runtime.core.internal.streaming.MemoryManager;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
 * {@link MemoryBoundByteBufferManager} implementation which pools instances for better performance.
 * <p>
 * Buffers are kept in separate pools depending on their capacity.
 * <p>
 * Idle capacity pools are automatically expired, but items in each pool are never reclaimed.
 * <p>
 * Unlike traditional pools, if a pool is exhausted then an ephemeral {@link ByteBuffer} will be produced. That instance
 * must still be returned through the {@link #deallocate(ByteBuffer)} method.
 *
 * @since 4.0
 */
public class PoolingByteBufferManager extends MemoryBoundByteBufferManager implements Disposable {

  private static final Logger LOGGER = getLogger(PoolingByteBufferManager.class);

  private final int size;

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
    this(new DefaultMemoryManager(), DEFAULT_BUFFER_POOL_SIZE, DEFAULT_BUFFER_BUCKET_SIZE, DEFAULT_MEMORY_EXHAUSTED_WAIT_TIME);
  }

  /**
   * Creates a new instance which allows the pool to grow up to 50% of calling {@link MemoryManager#getMaxMemory()} on the given
   * {@code memoryManager}, and has {@code waitTimeoutMillis} as wait timeout.
   *
   * @param memoryManager                    a {@link MemoryManager} used to determine the runtime's max memory
   * @param memoryExhaustedWaitTimeoutMillis how long to wait when the pool is exhausted
   */
  public PoolingByteBufferManager(MemoryManager memoryManager, int size, int bufferSize, long memoryExhaustedWaitTimeoutMillis) {
    super(memoryManager, memoryExhaustedWaitTimeoutMillis);
    this.size = size;
    defaultSizePool = newBufferPool(bufferSize);
  }

  private BufferPool newBufferPool(Integer capacity) {
    return new BufferPool(size, capacity);
  }

  private BufferPool getBufferPool(int capacity) {
    return capacity == DEFAULT_BUFFER_BUCKET_SIZE ? defaultSizePool : customSizePools.get(capacity);
  }

  @Override
  protected ByteBuffer doAllocate(int capacity) {
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

    super.dispose();
  }

  private class BufferPool {

    private final PoolService<ByteBuffer> pool;
    private final PoolObjectFactory<ByteBuffer> factory;
    private final int bufferCapacity;
    private final Set<Integer> ephemeralBufferIds = newSetFromMap(new ConcurrentHashMap<>());

    private BufferPool(int size, int bufferCapacity) {
      this.bufferCapacity = bufferCapacity;
      factory = new PoolObjectFactory<ByteBuffer>() {

        @Override
        public ByteBuffer create() {
          return allocateIfFits(bufferCapacity);
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
          PoolingByteBufferManager.super.deallocate(buffer);
        }
      };

      pool = new ConcurrentPool<>(new MultithreadConcurrentQueueCollection<>(size),
                                  factory, min(getRuntime().availableProcessors(), size), size, false);
    }

    private ByteBuffer take() {
      ByteBuffer buffer = pool.tryTake();
      if (buffer == null) {
        buffer = allocateIfFits(bufferCapacity);
        ephemeralBufferIds.add(identityHashCode(buffer));
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
        signalMemoryAvailable();
      }
    }

    private void close() {
      pool.close();
    }
  }
}
