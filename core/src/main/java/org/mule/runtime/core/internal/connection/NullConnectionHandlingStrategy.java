/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionHandler;

/**
 * A {@link ConnectionHandlerAdapter} which adds no behavior.
 * <p>
 * Each invokation to {@link #getConnectionHandler()} creates a new connection which is
 * closed when {@link ConnectionHandler#release()} is invoked.
 *
 * @param <Connection> the generic type of the connection being wrapped
 * @since 4.0
 */
final class NullConnectionHandlingStrategy<Connection> extends ConnectionHandlingStrategyAdapter<Connection>
{

    public NullConnectionHandlingStrategy(ConnectionProvider<Connection> connectionProvider, MuleContext muleContext)
    {
        super(connectionProvider, muleContext);
    }

    /**
     * Creates a new {@code Connection} by invoking {@link ConnectionProvider#connect()} on
     * {@link #connectionProvider}
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
        Connection connection = connectionProvider.connect();
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
