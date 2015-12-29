/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.internal.connection;

import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ConnectionHandlingStrategy;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;

/**
 * Base class for wrappers for {@link ConnectionProvider} instances
 *
 * @param <Config>     the generic type of the configs that the {@link #delegate} accepts
 * @param <Connection> the generic type of the connections that the {@link #delegate} produces
 * @since 4.0
 */
public abstract class ConnectionProviderWrapper<Config, Connection> implements ConnectionProvider<Config, Connection>
{

    private final ConnectionProvider<Config, Connection> delegate;

    /**
     * Creates a new instance which wraps the {@code delegate}
     *
     * @param delegate the {@link ConnectionProvider} to be wrapped
     */
    ConnectionProviderWrapper(ConnectionProvider<Config, Connection> delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public Connection connect(Config config) throws ConnectionException
    {
        return delegate.connect(config);
    }

    @Override
    public void disconnect(Connection connection)
    {
        delegate.disconnect(connection);
    }

    @Override
    public ConnectionHandlingStrategy<Connection> getHandlingStrategy(ConnectionHandlingStrategyFactory<Config, Connection> handlingStrategyFactory)
    {
        return delegate.getHandlingStrategy(handlingStrategyFactory);
    }

    public ConnectionProvider<Config, Connection> getDelegate()
    {
        return delegate;
    }
}
