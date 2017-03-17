/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.pool.impl.GenericObjectPool.DEFAULT_MAX_IDLE;
import static org.apache.commons.pool.impl.GenericObjectPool.DEFAULT_MAX_WAIT;
import static org.apache.commons.pool.impl.GenericObjectPool.WHEN_EXHAUSTED_GROW;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;

import java.nio.ByteBuffer;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;

/**
 * {@link ByteBufferManager} implementation which pools instances for better performance.
 * <p>
 * Buffers are kept in separate pools depending on their capacity.
 * <p>
 * Idle buffers and capacity pools are automatically expired.
 *
 * @since 4.0
 */
public class PoolingByteBufferManager implements ByteBufferManager, Disposable {

  private static final Logger LOGGER = getLogger(PoolingByteBufferManager.class);
  private static final long ONE_MINUTE = MINUTES.toMillis(1);

  private final LoadingCache<Integer, ObjectPool<ByteBuffer>> pools = CacheBuilder.newBuilder()
      .expireAfterAccess(1, MINUTES)
      .removalListener((RemovalListener<Integer, ObjectPool<ByteBuffer>>) notification -> {
        try {
          notification.getValue().close();
        } catch (Exception e) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Found exception trying to dispose buffer pool for capacity " + notification.getKey(), e);
          }
        }
      }).build(new CacheLoader<Integer, ObjectPool<ByteBuffer>>() {

        @Override
        public ObjectPool<ByteBuffer> load(Integer capacity) throws Exception {
          return createPool(capacity);
        }
      });

  /**
   * {@inheritDoc}
   */
  @Override
  public ByteBuffer allocate(int capacity) {
    try {
      return pools.getUnchecked(capacity).borrowObject();
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not allocate byte buffer"), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deallocate(ByteBuffer byteBuffer) {
    int capacity = byteBuffer.capacity();
    ObjectPool<ByteBuffer> pool = pools.getIfPresent(capacity);
    if (pool != null) {
      try {
        pool.returnObject(byteBuffer);
      } catch (Exception e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Could not deallocate buffer of capacity " + capacity, e);
        }
      }
    }
  }

  @Override
  public void dispose() {
    pools.invalidateAll();
  }

  private ObjectPool<ByteBuffer> createPool(int capacity) {
    GenericObjectPool.Config config = new GenericObjectPool.Config();
    config.maxIdle = DEFAULT_MAX_IDLE;
    config.maxActive = -1;
    config.maxWait = DEFAULT_MAX_WAIT;
    config.whenExhaustedAction = WHEN_EXHAUSTED_GROW;
    config.minEvictableIdleTimeMillis = ONE_MINUTE;
    config.timeBetweenEvictionRunsMillis = ONE_MINUTE;
    config.testOnBorrow = false;
    config.testOnReturn = false;
    config.testWhileIdle = false;
    GenericObjectPool genericPool = new GenericObjectPool(new ByteBufferObjectFactory(capacity), config);

    return genericPool;
  }

  private class ByteBufferObjectFactory implements PoolableObjectFactory<ByteBuffer> {

    private final int capacity;

    private ByteBufferObjectFactory(int capacity) {
      this.capacity = capacity;
    }

    @Override
    public ByteBuffer makeObject() throws Exception {
      return ByteBuffer.allocate(capacity);
    }

    @Override
    public void destroyObject(ByteBuffer obj) throws Exception {
      obj.clear();
    }

    @Override
    public boolean validateObject(ByteBuffer obj) {
      return false;
    }

    @Override
    public void activateObject(ByteBuffer obj) throws Exception {
      obj.clear();
    }

    @Override
    public void passivateObject(ByteBuffer obj) throws Exception {

    }
  }
}
