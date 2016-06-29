/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.PoolingListener;
import org.mule.runtime.core.api.MuleContext;

/**
 * Default implementation of {@link ConnectionHandlingStrategyFactory}.
 *
 * @param <Connection> the generic type of the connections that will be produced
 * @since 4.0
 */
final class DefaultConnectionHandlingStrategyFactory<Connection> implements ConnectionHandlingStrategyFactory<Connection>
{

    private final ConnectionProvider<Connection> connectionProvider;
    private final MuleContext muleContext;
    private final PoolingProfile poolingProfile;

    /**
     * Creates a new instance
     *
     * @param connectionProvider the {@link ConnectionProvider} that will be used to manage connections
     * @param poolingProfile     the {@link PoolingProfile} that will be used to configure the pool of connections
     * @param muleContext        the owning  {@link MuleContext}
     */
    DefaultConnectionHandlingStrategyFactory(ConnectionProvider<Connection> connectionProvider,
                                             PoolingProfile poolingProfile,
                                             MuleContext muleContext)
    {
        this.connectionProvider = connectionProvider;
        this.poolingProfile = poolingProfile;
        this.muleContext = muleContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy supportsPooling()
    {
        return supportsPooling(new NullPoolingListener<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<Connection> supportsPooling(PoolingListener<Connection> poolingListener)
    {
        return poolingProfile.isDisabled() ? none() : createPoolingStrategy(poolingListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<Connection> requiresPooling()
    {
        if (poolingProfile.isDisabled())
        {
            throw new IllegalArgumentException("The selected connection management strategy requires pooling but the supplied pooling profile " +
                                               "is attempting to disable pooling. Supply a valid PoolingProfile or choose a different management strategy.");
        }

        return requiresPooling(new NullPoolingListener<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<Connection> requiresPooling(PoolingListener<Connection> poolingListener)
    {
        return createPoolingStrategy(poolingListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<Connection> cached()
    {
        return new CachedConnectionHandlingStrategy<>(connectionProvider, muleContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionHandlingStrategy<Connection> none()
    {
        return new NullConnectionHandlingStrategy<>(connectionProvider, muleContext);
    }

    private PoolingConnectionHandlingStrategy<Connection> createPoolingStrategy(PoolingListener<Connection> poolingListener)
    {
        return new PoolingConnectionHandlingStrategy<>(connectionProvider, poolingProfile, poolingListener, muleContext);
    }
}
