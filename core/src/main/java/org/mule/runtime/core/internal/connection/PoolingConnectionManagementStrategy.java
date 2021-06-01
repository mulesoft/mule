/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.PoolingListener;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;

import java.util.NoSuchElementException;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * A {@link ConnectionManagementStrategy} which returns connections obtained from a {@link #pool}
 *
 * @param <C> the generic type of the connections to be managed
 * @since 4.0
 */
final class PoolingConnectionManagementStrategy<C> extends ConnectionManagementStrategy<C> {

  private final PoolingProfile poolingProfile;
  private final ObjectPool<C> pool;
  private final PoolingListener<C> poolingListener;

  /**
   * Creates a new instance
   *
   * @param connectionProvider the {@link ConnectionProvider} used to manage the connections
   * @param poolingProfile the {@link PoolingProfile} which configures the {@link #pool}
   * @param poolingListener a {@link PoolingListener}
   * @param muleContext the application's {@link MuleContext}
   */
  PoolingConnectionManagementStrategy(ConnectionProvider<C> connectionProvider, PoolingProfile poolingProfile,
                                      PoolingListener<C> poolingListener, MuleContext muleContext) {
    super(connectionProvider, muleContext);
    this.poolingProfile = poolingProfile;
    this.poolingListener = poolingListener;
    pool = createPool();
  }

  /**
   * Returns a {@link ConnectionHandler} which wraps a connection obtained from the {@link #pool}
   *
   * @return a {@link ConnectionHandler}
   * @throws ConnectionException if the connection could not be obtained
   */
  @Override
  public ConnectionHandler<C> getConnectionHandler() throws ConnectionException {
    try {
      return new PoolingConnectionHandler<>(borrowConnection(), pool, poolingListener, connectionProvider);
    } catch (ConnectionException e) {
      throw e;
    } catch (NoSuchElementException e) {
      throw new ConnectionException("Connection pool is exhausted", e);
    } catch (Exception e) {
      throw new ConnectionException("An exception was found trying to obtain a connection: " + e.getMessage(), e);
    }
  }

  private C borrowConnection() throws Exception {
    C connection = pool.borrowObject();
    try {
      poolingListener.onBorrow(connection);
    } catch (Exception e) {
      pool.invalidateObject(connection);
      throw e;
    }

    return connection;
  }

  /**
   * Closes the pool, causing the contained connections to be closed as well.
   *
   * @throws MuleException
   */
  // TODO: MULE-9082 - pool.close() doesn't destroy unreturned connections
  @Override
  public void close() throws MuleException {
    try {
      pool.close();
    } catch (Exception e) {
      throw new DefaultMuleException(createStaticMessage("Could not close connection pool"), e);
    }
  }

  private ObjectPool<C> createPool() {
    GenericObjectPool.Config config = new GenericObjectPool.Config();
    config.maxIdle = poolingProfile.getMaxIdle();
    config.maxActive = poolingProfile.getMaxActive();
    config.maxWait = poolingProfile.getMaxWait();
    config.whenExhaustedAction = (byte) poolingProfile.getExhaustedAction();
    config.minEvictableIdleTimeMillis = poolingProfile.getMinEvictionMillis();
    config.timeBetweenEvictionRunsMillis = poolingProfile.getEvictionCheckIntervalMillis();
    GenericObjectPool genericPool = new GenericObjectPool(new ObjectFactoryAdapter(), config);

    return genericPool;
  }

  public PoolingProfile getPoolingProfile() {
    return poolingProfile;
  }

  private class ObjectFactoryAdapter implements PoolableObjectFactory<C> {

    @Override
    public C makeObject() throws Exception {
      return connectionProvider.connect();
    }

    @Override
    public void destroyObject(C connection) throws Exception {
      connectionProvider.disconnect(connection);
    }

    @Override
    public boolean validateObject(C obj) {
      return false;
    }

    @Override
    public void activateObject(C connection) throws Exception {}

    @Override
    public void passivateObject(C connection) throws Exception {}
  }
}
