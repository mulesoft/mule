/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.assertNotStopping;
import static reactor.core.Exceptions.unwrap;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.util.func.CheckedSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ConnectionHandlerAdapter} which always returns the same connection (therefore cached), which is not established until
 * {@link #getConnection()} is first invoked.
 * <p/>
 * This implementation is thread-safe.
 *
 * @param <C> the generic type of the connection being wrapped
 * @since 4.0
 */
final class CachedConnectionHandler<C> implements ConnectionHandlerAdapter<C> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CachedConnectionHandler.class);

  private final ConnectionProvider<C> connectionProvider;
  private final MuleContext muleContext;
  private LazyValue<C> connection;

  /**
   * Creates a new instance
   *
   * @param connectionProvider the {@link ConnectionProvider} to be used to managed the connection
   * @param muleContext the owning {@link MuleContext}
   */
  public CachedConnectionHandler(ConnectionProvider<C> connectionProvider, MuleContext muleContext) {
    this.connectionProvider = connectionProvider;
    this.muleContext = muleContext;
    lazyConnect();
  }

  /**
   * On the first invocation to this method, a connection is established using the provided {@link #connectionProvider}. That
   * connection is cached and returned.
   * <p/>
   * Following invocations simply return the same connection.
   *
   * @return a {@code Connection}
   * @throws ConnectionException if a {@code Connection} could not be obtained
   * @throws IllegalStateException if the first invocation is executed while the {@link #muleContext} is stopping or stopped
   */
  @Override
  public C getConnection() throws ConnectionException {
    try {
      return connection.get();
    } catch (Throwable t) {
      t = unwrap(t);
      if (t instanceof ConnectionException) {
        throw (ConnectionException) t;
      }

      throw new ConnectionException(t.getMessage(), t);
    }
  }

  private C createConnection() throws ConnectionException {
    assertNotStopping(muleContext, "Mule is shutting down... Cannot establish new connections");
    return connectionProvider.connect();
  }

  /**
   * This implementation doesn't require the concept of release. This method does nothing
   */
  @Override
  public void release() {
    // no-op
  }

  /**
   * Disconnects the wrapped connection and clears the cache
   *
   * @throws MuleException in case of error
   */
  @Override
  public void close() throws MuleException {
    disconnectAndCleanConnection();
  }

  @Override
  public void invalidate() {
    disconnectAndCleanConnection();
    lazyConnect();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionProvider<C> getConnectionProvider() {
    return connectionProvider;
  }

  private void disconnectAndCleanConnection() {
    connection.ifComputed(c -> {
      try {
        connectionProvider.disconnect(c);
      } catch (Exception e) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(String.format("Error disconnecting cached connection %s. %s", c, e.getMessage()), e);
        }
      } finally {
        connection = null;
      }
    });
  }

  private void lazyConnect() {
    connection = new LazyValue<>((CheckedSupplier<C>) this::createConnection);
  }
}
