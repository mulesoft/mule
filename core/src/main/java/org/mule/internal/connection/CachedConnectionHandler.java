/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.internal.connection;

import static org.mule.api.lifecycle.LifecycleUtils.assertNotStopping;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionProvider;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A {@link ConnectionHandlerAdapter} which always returns the same connection (therefore cached),
 * which is not established until {@link #getConnection()} is first invoked.
 * <p>
 * This implementation is thread-safe.
 *
 * @param <Config>     the generic type of the config that owns {@code this} managed connection
 * @param <Connection> the generic type of the connection being wrapped
 * @since 4.0
 */
final class CachedConnectionHandler<Config, Connection> implements ConnectionHandlerAdapter<Connection>
{

    private final Config config;
    private final ConnectionProvider<Config, Connection> connectionProvider;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    private final MuleContext muleContext;

    private Connection connection;

    /**
     * Creates a new instance
     *
     * @param config             the {@code Config} that owns the wrapped connection
     * @param connectionProvider the {@link ConnectionProvider} to be used to managed the connection
     * @param muleContext        the owning {@link MuleContext}
     */
    public CachedConnectionHandler(Config config, ConnectionProvider<Config, Connection> connectionProvider, MuleContext muleContext)
    {
        this.config = config;
        this.connectionProvider = connectionProvider;
        this.muleContext = muleContext;
    }

    /**
     * On the first invokation to this method, a connection is established using the provided
     * {@link #connectionProvider}. That connection is cached and returned.
     * <p>
     * Following invokations simply return the same connection.
     *
     * @return a {@code Connection}
     * @throws ConnectionException   if a {@code Connection} could not be obtained
     * @throws IllegalStateException if the first invokation is executed while the {@link #muleContext} is stopping or stopped
     */
    @Override
    public Connection getConnection() throws ConnectionException
    {
        readLock.lock();
        try
        {
            if (connection != null)
            {
                return connection;
            }
        }
        finally
        {
            readLock.unlock();
        }

        writeLock.lock();
        try
        {
            //check another thread didn't beat us to it
            if (connection != null)
            {
                return connection;
            }

            assertNotStopping(muleContext, "Mule is shutting down... Cannot establish new connections");
            return connection = connectionProvider.connect(config);
        }
        finally
        {
            writeLock.unlock();
        }
    }

    /**
     * This implementation doesn't require the concept of release.
     * This method does nothing
     */
    @Override
    public void release()
    {
        //no-op
    }

    /**
     * Disconnects the wrapped connection and clears the cache
     *
     * @throws Exception in case of error
     */
    @Override
    public void close() throws MuleException
    {
        writeLock.lock();
        try
        {
            if (connectionProvider != null && connection != null)
            {
                connectionProvider.disconnect(connection);
            }
        }
        finally
        {
            connection = null;
            writeLock.unlock();
        }
    }
}
