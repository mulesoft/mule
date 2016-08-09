/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.PoolingListener;

import org.apache.commons.pool.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ConnectionHandlerAdapter} which wraps a {@code Connection} obtained from a {@link #pool}.
 *
 * @param <C> the generic type of the connection to be returned
 * @since 4.0
 */
final class PoolingConnectionHandler<C> implements ConnectionHandlerAdapter<C> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PoolingConnectionHandler.class);

  private final C connection;
  private final ObjectPool<C> pool;
  private final PoolingListener poolingListener;

  /**
   * Creates a new instance
   *
   * @param connection the connection to be wrapped
   * @param pool the pool from which the {@code connection} was obtained and to which it has to be returned
   */
  PoolingConnectionHandler(C connection, ObjectPool<C> pool, PoolingListener poolingListener) {
    this.connection = connection;
    this.pool = pool;
    this.poolingListener = poolingListener;
  }

  /**
   * @return the {@link #connection}
   */
  @Override
  public C getConnection() throws ConnectionException {
    return connection;
  }

  /**
   * Returns the {@link #connection} to the {@link #pool}
   */
  @Override
  public void release() {
    boolean returnAttempted = false;
    try {
      poolingListener.onReturn(connection);

      returnAttempted = true;
      pool.returnObject(connection);
    } catch (Exception e) {
      LOGGER.warn("Could not return connection to the pool. Connection has been destroyed", e);
    } finally {
      if (!returnAttempted) {
        try {
          pool.invalidateObject(connection);
        } catch (Exception e) {
          LOGGER.warn("Exception was found trying to invalidate connection of type " + connection.getClass().getName(), e);
        }
      }
    }
  }

  /**
   * Does nothing for this implementation. Connections are only closed when the pool is.
   */
  @Override
  public void close() throws MuleException {

  }
}
