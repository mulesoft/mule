/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.internal.connection.ConnectionUtils.logPoolStatus;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.PoolingListener;
import org.mule.runtime.api.exception.MuleException;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.pool2.impl.GenericObjectPool;
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

  private C connection;
  private final GenericObjectPool<C> pool;
  private final String poolId;
  private final PoolingListener poolingListener;
  private final ConnectionProvider connectionProvider;
  private final AtomicBoolean released = new AtomicBoolean(false);

  /**
   * Creates a new instance
   *
   * @param connection the connection to be wrapped
   * @param pool       the pool from which the {@code connection} was obtained and to which it has to be returned
   */
  PoolingConnectionHandler(C connection, GenericObjectPool<C> pool, String poolId, PoolingListener poolingListener,
                           ConnectionProvider connectionProvider) {
    this.connection = connection;
    this.pool = pool;
    this.poolId = poolId;
    this.poolingListener = poolingListener;
    this.connectionProvider = connectionProvider;
  }

  /**
   * @return the {@link #connection}
   */
  @Override
  public C getConnection() throws ConnectionException {
    checkState(connection != null, "Connection has been either released or invalidated");
    return connection;
  }

  /**
   * Returns the {@link #connection} to the {@link #pool}
   */
  @Override
  public void release() {
    if (connection == null || released.getAndSet(true)) {
      return;
    }

    boolean returnAttempted = false;
    try {
      LOGGER.debug("Returning back connection {} to pool {}", connection.toString(), poolId);
      poolingListener.onReturn(connection);

      pool.returnObject(connection);
      logPoolStatus(LOGGER, pool, poolId);
      returnAttempted = true;
    } catch (Exception e) {
      LOGGER.warn("Could not return connection to the pool. Connection will be terminated", e);
    } finally {
      try {
        if (!returnAttempted) {
          invalidate();
        }
      } finally {
        connection = null;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void invalidate() {
    try {
      LOGGER.debug("Invalidating connection {} from pool {}", connection.toString(), poolId);
      pool.invalidateObject(connection);
      logPoolStatus(LOGGER, pool, poolId);
    } catch (Exception e) {
      LOGGER.warn("Exception was thrown trying to invalidate connection of type " + connection.getClass().getName(), e);
    } finally {
      connection = null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /**
   * Does nothing for this implementation. Connections are only closed when the pool is.
   */
  @Override
  public void close() throws MuleException {

  }
}
