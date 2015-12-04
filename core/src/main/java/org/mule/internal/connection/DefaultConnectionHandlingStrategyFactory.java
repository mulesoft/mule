/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.internal.connection;

import org.mule.api.MuleContext;
import org.mule.api.config.PoolingProfile;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ConnectionHandlingStrategy;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.api.connection.PoolingListener;

/**
 * Default implementation of {@link ConnectionHandlingStrategyFactory}.
 * <p>
 * This implementation is stateful an is tightly associated to a {@link #config},
 * {@link #connectionProvider} and {@link #muleContext}.
 *
 * @param <Config>     the generic type of the config for which connections will be produced
 * @param <Connection> the generic type of the connections that will be produced
 * @since 4.0
 */
final class DefaultConnectionHandlingStrategyFactory<Config, Connection> implements ConnectionHandlingStrategyFactory<Config, Connection>
{

    private final Config config;
    private final ConnectionProvider<Config, Connection> connectionProvider;
    private final MuleContext muleContext;

    /**
     * Creates a new instance
     *
     * @param config             the config for which we try to create connections
     * @param connectionProvider the {@link ConnectionProvider} that will be used to manage connections
     * @param muleContext        the owning  {@link MuleContext}
     */
    DefaultConnectionHandlingStrategyFactory(Config config, ConnectionProvider<Config, Connection> connectionProvider, MuleContext muleContext)
    {
        this.config = config;
        this.connectionProvider = connectionProvider;
        this.muleContext = muleContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy supportsPooling(PoolingProfile defaultPoolingProfile)
    {
        return supportsPooling(defaultPoolingProfile, new NullPoolingListener<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<Connection> supportsPooling(PoolingProfile defaultPoolingProfile, PoolingListener<Config, Connection> poolingListener)
    {
        return defaultPoolingProfile.isDisabled()
               ? none()
               : createPoolingStrategy(defaultPoolingProfile, poolingListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<Connection> requiresPooling(PoolingProfile defaultPoolingProfile)
    {
        return requiresPooling(defaultPoolingProfile, new NullPoolingListener<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<Connection> requiresPooling(PoolingProfile defaultPoolingProfile, PoolingListener<Config, Connection> poolingListener)
    {
        if (defaultPoolingProfile.isDisabled())
        {
            throw new IllegalArgumentException("The selected connection management strategy requires pooling but the supplied pooling profile " +
                                               "is attempting to disable pooling. Supply a valid PoolingProfile or choose a different management strategy.");
        }

        return createPoolingStrategy(defaultPoolingProfile, poolingListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<Connection> cached()
    {
        return new CachedConnectionHandlingStrategy<>(config, connectionProvider, muleContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<Connection> none()
    {
        return new NullConnectionHandlingStrategy<>(config, connectionProvider, muleContext);
    }

    private PoolingConnectionHandlingStrategy<Config, Connection> createPoolingStrategy(PoolingProfile defaultPoolingProfile, PoolingListener<Config, Connection> poolingListener)
    {
        return new PoolingConnectionHandlingStrategy<>(config, connectionProvider, defaultPoolingProfile, poolingListener,  muleContext);
    }
}
