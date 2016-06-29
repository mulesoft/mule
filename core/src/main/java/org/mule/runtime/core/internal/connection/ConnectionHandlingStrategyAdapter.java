/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.Closeable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;

/**
 * Base class for {@link ConnectionHandlingStrategy} implementations which contains state
 * and expands its contract with non API functionality
 *
 * @param <Connection> the generic type of the connections to be managed
 * @since 4.0
 */
public abstract class ConnectionHandlingStrategyAdapter<Connection> implements ConnectionHandlingStrategy<Connection>, Closeable
{

    protected final ConnectionProvider<Connection> connectionProvider;
    protected final MuleContext muleContext;

    /**
     * Creates a new instance
     *
     * @param connectionProvider the {@link ConnectionProvider} which will be used to manage the connections
     * @param muleContext        the application's {@link MuleContext}
     */
    ConnectionHandlingStrategyAdapter(ConnectionProvider<Connection> connectionProvider, MuleContext muleContext)
    {
        this.connectionProvider = connectionProvider;
        this.muleContext = muleContext;
    }

    /**
     * Closes all connections and resources allocated through {@code this} instance.
     *
     * @throws MuleException if an exception was found closing the connections
     */
    @Override
    public abstract void close() throws MuleException;
}
