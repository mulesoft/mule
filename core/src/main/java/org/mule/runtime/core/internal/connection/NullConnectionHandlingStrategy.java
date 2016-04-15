/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ConnectionHandler;

/**
 * A {@link ConnectionHandlerAdapter} which adds no behavior.
 * <p>
 * Each invokation to {@link #getConnectionHandler()} creates a new connection which is
 * closed when {@link ConnectionHandler#release()} is invoked.
 *
 * @param <Config>     the generic type of the config that owns {@code this} managed connection
 * @param <Connection> the generic type of the connection being wrapped
 * @since 4.0
 */
final class NullConnectionHandlingStrategy<Config, Connection> extends ConnectionHandlingStrategyAdapter<Config, Connection>
{

    public NullConnectionHandlingStrategy(Config config, ConnectionProvider<Config, Connection> connectionProvider, MuleContext muleContext)
    {
        super(config, connectionProvider, muleContext);
    }

    /**
     * Creates a new {@code Connection} by invoking {@link ConnectionProvider#connect(Object)} on
     * {@link #connectionProvider} using {@link #config} as argument.
     * <p>
     * The connection will be closed when {@link ConnectionHandler#release()} is invoked on
     * the returned {@link ConnectionHandler}
     *
     * @return a {@link ConnectionHandler}
     * @throws ConnectionException if the connection could not be established
     */
    @Override
    public ConnectionHandler<Connection> getConnectionHandler() throws ConnectionException
    {
        Connection connection = connectionProvider.connect(config);
        return new PassThroughConnectionHandler<>(connection, connectionProvider);
    }

    /**
     * This method does nothing for this implementation. The connection will be closed
     * via {@link ConnectionHandler#release()} or {@link ConnectionHandlerAdapter#close()}
     */
    @Override
    public void close() throws MuleException
    {

    }
}
