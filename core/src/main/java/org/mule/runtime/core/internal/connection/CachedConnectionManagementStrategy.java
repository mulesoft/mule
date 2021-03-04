/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.assertNotStopping;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.unwrap;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.func.CheckedSupplier;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;

/**
 * A {@link ConnectionManagementStrategy} which lazily creates and caches connections, so that the same instance is returned each
 * time that one is required.
 * <p/>
 * When {@link ConnectionHandler#release()} is invoked on the instances returned by {@link #getConnectionHandler()}, the
 * connection is not actually closed. It will only be disconnected when {@link #close()} is called.
 *
 * @param <C> the generic type of the connections being managed
 * @since 4.0
 */
final class CachedConnectionManagementStrategy<C> extends ConnectionManagementStrategy<C> {

  private static final Logger LOGGER = getLogger(CachedConnectionManagementStrategy.class);

  private final Lock connectionLock = new ReentrantLock();
  private LazyValue<ConnectionHandlerAdapter<C>> connectionHandler;

  /**
   * Creates a new instance
   *
   * @param connectionProvider the {@link ConnectionProvider} used to manage the connections
   * @param muleContext        the owning {@link MuleContext}
   */
  CachedConnectionManagementStrategy(ConnectionProvider<C> connectionProvider, MuleContext muleContext) {
    super(connectionProvider, muleContext);
    lazyConnect();
  }

  /**
   * Returns the cached connection
   *
   * @return a {@link ConnectionHandler}
   * @throws ConnectionException if the connection could not be established
   */
  @Override
  public ConnectionHandler<C> getConnectionHandler() throws ConnectionException {
    try {
      return connectionHandler.get();
    } catch (Throwable t) {
      t = unwrap(t);
      if (t instanceof ConnectionException) {
        throw (ConnectionException) t;
      }

      throw new ConnectionException(t.getMessage(), t);
    }
  }

  /**
   * Invokes {@link ConnectionHandlerAdapter#close()} on the cached connection
   *
   * @throws MuleException if an exception is found trying to close the connection
   */
  @Override
  public void close() throws MuleException {
    connectionHandler.ifComputed(this::close);
  }

  private synchronized void lazyConnect() {
    connectionLock.lock();
    try {
      connectionHandler = new LazyValue<>((CheckedSupplier<ConnectionHandlerAdapter<C>>) this::createConnection);
    } finally {
      connectionLock.unlock();
    }
  }

  private ConnectionHandlerAdapter<C> createConnection() throws ConnectionException {
    assertNotStopping(muleContext, "Mule is shutting down... Cannot establish new connections");
    return new CachedConnectionHandler<>(connectionProvider.connect(), this::invalidate, connectionProvider);
  }

  private void close(ConnectionHandlerAdapter<C> connectionHandler) {
    try {
      connectionHandler.close();
    } catch (Exception e) {
      LOGGER.warn("Error closing cached connection", e);
    }
  }

  private void invalidate(ConnectionHandlerAdapter<C> connection) {
    try {
      close(connection);
    } finally {
      connectionLock.lock();
      try {
        lazyConnect();
      } finally {
        connectionLock.unlock();
      }
    }
  }
}
