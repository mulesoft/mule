/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.internal.connection;

import org.mule.api.connection.ConnectionHandlingStrategy;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.api.connection.PoolingListener;

/**
 * Base class for {@link ConnectionHandlingStrategyFactory} decorators
 *
 * @param <Config>     the generic type of the config for which connections will be produced
 * @param <Connection> the generic type of the connections that will be produced
 * @since 4.0
 */
abstract class ConnectionHandlingStrategyFactoryWrapper<Config, Connection> implements ConnectionHandlingStrategyFactory<Config, Connection>
{

    protected final ConnectionHandlingStrategyFactory<Config, Connection> delegate;

    /**
     * Creates a new instance
     *
     * @param delegate the {@link ConnectionHandlingStrategyFactory} to be decorated
     */
    ConnectionHandlingStrategyFactoryWrapper(ConnectionHandlingStrategyFactory<Config, Connection> delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public ConnectionHandlingStrategy<Connection> supportsPooling()
    {
        return delegate.supportsPooling();
    }

    @Override
    public ConnectionHandlingStrategy<Connection> supportsPooling(PoolingListener<Config, Connection> poolingListener)
    {
        return delegate.supportsPooling(poolingListener);
    }

    @Override
    public ConnectionHandlingStrategy<Connection> requiresPooling()
    {
        return delegate.requiresPooling();
    }

    @Override
    public ConnectionHandlingStrategy<Connection> requiresPooling(PoolingListener<Config, Connection> poolingListener)
    {
        return delegate.requiresPooling(poolingListener);
    }

    @Override
    public ConnectionHandlingStrategy<Connection> cached()
    {
        return delegate.cached();
    }

    @Override
    public ConnectionHandlingStrategy<Connection> none()
    {
        return delegate.none();
    }
}
