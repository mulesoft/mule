/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.internal.connection;

import org.mule.api.config.PoolingProfile;
import org.mule.api.connection.ConnectionHandlingStrategy;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.PoolingListener;

/**
 * A {@link ConnectionProviderWrapper} which decorates the {@link #delegate}
 * with a user configured {@link PoolingProfile}.
 * <p/>
 * The purpose of this wrapper is having the {@link #getHandlingStrategy(ConnectionHandlingStrategyFactory)}
 * method use the configured {@link #poolingProfile} instead of the default included
 * in the {@link #delegate}
 * <p/>
 * If a {@link #poolingProfile} is not supplied (meaning, it is {@code null}), then the
 * default {@link #delegate} behavior is applied.
 *
 * @since 4.0
 */
public final class PooledConnectionProviderWrapper<Config, Connection> extends ConnectionProviderWrapper<Config, Connection>
{

    private final PoolingProfile poolingProfile;

    /**
     * Creates a new instance
     *
     * @param delegate       the {@link ConnectionProvider} to be wrapped
     * @param poolingProfile a nullable {@link PoolingProfile}
     */
    public PooledConnectionProviderWrapper(ConnectionProvider<Config, Connection> delegate, PoolingProfile poolingProfile)
    {
        super(delegate);
        this.poolingProfile = poolingProfile;
    }

    /**
     * If {@link #poolingProfile} is not {@code null} and the delegate wants to invoke
     * {@link ConnectionHandlingStrategyFactory#requiresPooling(PoolingProfile)} or
     * {@link ConnectionHandlingStrategyFactory#supportsPooling(PoolingProfile)}, then this method
     * makes those invokations using the supplied {@link #poolingProfile}.
     * <p/>
     * In any other case, the default {@link #delegate} behavior is applied
     *
     * @param handlingStrategyFactory a {@link ConnectionHandlingStrategyFactory}
     * @return a {@link ConnectionHandlingStrategy}
     */
    @Override
    public ConnectionHandlingStrategy<Connection> getHandlingStrategy(ConnectionHandlingStrategyFactory<Config, Connection> handlingStrategyFactory)
    {
        ConnectionHandlingStrategyFactory<Config, Connection> factoryDecorator = new ConnectionHandlingStrategyFactoryWrapper<Config, Connection>(handlingStrategyFactory)
        {
            public ConnectionHandlingStrategy<Connection> supportsPooling(PoolingProfile defaultPoolingProfile, PoolingListener<Config, Connection> poolingListener)
            {
                return super.supportsPooling(resolvePoolingProfile(defaultPoolingProfile), poolingListener);
            }

            @Override
            public ConnectionHandlingStrategy<Connection> supportsPooling(PoolingProfile defaultPoolingProfile)
            {
                return super.supportsPooling(resolvePoolingProfile(defaultPoolingProfile));
            }

            @Override
            public ConnectionHandlingStrategy<Connection> requiresPooling(PoolingProfile defaultPoolingProfile, PoolingListener<Config, Connection> poolingListener)
            {
                return super.requiresPooling(resolvePoolingProfile(defaultPoolingProfile), poolingListener);
            }

            @Override
            public ConnectionHandlingStrategy<Connection> requiresPooling(PoolingProfile defaultPoolingProfile)
            {
                return super.requiresPooling(resolvePoolingProfile(defaultPoolingProfile));
            }

            private PoolingProfile resolvePoolingProfile(PoolingProfile defaultPoolingProfile)
            {
                return poolingProfile != null ? poolingProfile : defaultPoolingProfile;
            }
        };

        return super.getHandlingStrategy(factoryDecorator);
    }
}
