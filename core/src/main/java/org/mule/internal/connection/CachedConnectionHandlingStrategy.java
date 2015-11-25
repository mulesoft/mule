/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.internal.connection;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ConnectionHandler;

/**
 * A {@link ConnectionHandlingStrategyAdapter} which is associated to a {@link #config}.
 * A connection is lazily created and cached, so that the same instance is returned
 * each time the {@link #config} requires a connection.
 * <p>
 * When {@link ConnectionHandler#release()} is invoked on the instances returned
 * by {@link #getConnectionHandler()}, the connection is not actually closed. It
 * will only be disconnected when {@link #close()} is called.
 *
 * @param <Config>     the generic type of the configuration associated to this strategy
 * @param <Connection> the generic type of the connections being managed
 * @since 4.0
 */
final class CachedConnectionHandlingStrategy<Config, Connection> extends ConnectionHandlingStrategyAdapter<Config, Connection>
{

    private final ConnectionHandlerAdapter<Connection> connection;

    /**
     * Creates a new instance
     *
     * @param config             the config to which the {@code connectionProvider} is bounded
     * @param connectionProvider the {@link ConnectionProvider} used to manage the connections
     * @param muleContext        the owning {@link MuleContext}
     */
    CachedConnectionHandlingStrategy(Config config, ConnectionProvider<Config, Connection> connectionProvider, MuleContext muleContext)
    {
        super(config, connectionProvider, muleContext);
        connection = new CachedConnectionHandler<>(config, connectionProvider, muleContext);
    }

    /**
     * Returns the cached connection
     *
     * @return a {@link ConnectionHandler}
     * @throws ConnectionException if the connection could not be established
     */
    @Override
    public ConnectionHandler<Connection> getConnectionHandler() throws ConnectionException
    {
        return connection;
    }

    /**
     * Invokes {@link ConnectionHandlerAdapter#close()} on the cached connection
     *
     * @throws MuleException if an exception is found trying to close the connection
     */
    @Override
    public void close() throws MuleException
    {
        connection.close();
    }
}
