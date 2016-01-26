/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.model.property;

import org.mule.api.config.PoolingProfile;
import org.mule.api.connection.ConnectionHandlingStrategy;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.PoolingListener;
import org.mule.extension.api.introspection.PoolingSupport;
import org.mule.extension.api.introspection.property.ConnectionHandlingTypeModelProperty;

/**
 * Immutable implementation of {@link ConnectionHandlingTypeModelProperty}
 *
 * @since 4.0
 */
public final class ImmutableConnectionHandlingTypeModelProperty implements ConnectionHandlingTypeModelProperty
{

    private boolean cached = false;
    private PoolingSupport poolingSupport = PoolingSupport.NOT_SUPPORTED;
    private boolean none = false;

    public ImmutableConnectionHandlingTypeModelProperty(ConnectionProvider connectionProvider)
    {
        connectionProvider.getHandlingStrategy(new ConnectionHandlingStrategyFactory()
        {
            @Override
            public ConnectionHandlingStrategy supportsPooling(PoolingProfile defaultPoolingProfile)
            {
                poolingSupport = PoolingSupport.SUPPORTED;
                return null;
            }

            @Override
            public ConnectionHandlingStrategy supportsPooling(PoolingProfile defaultPoolingProfile, PoolingListener poolingListener)
            {
                return supportsPooling(defaultPoolingProfile);
            }

            @Override
            public ConnectionHandlingStrategy requiresPooling(PoolingProfile defaultPoolingProfile)
            {
                poolingSupport = PoolingSupport.REQUIRED;
                return null;
            }

            @Override
            public ConnectionHandlingStrategy requiresPooling(PoolingProfile defaultPoolingProfile, PoolingListener poolingListener)
            {
                return requiresPooling(defaultPoolingProfile);
            }

            @Override
            public ConnectionHandlingStrategy cached()
            {
                cached = true;
                return null;
            }

            @Override
            public ConnectionHandlingStrategy none()
            {
                none = true;
                return null;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCached()
    {
        return cached;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNone()
    {
        return none;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPooled()
    {
        return !poolingSupport.equals(PoolingSupport.NOT_SUPPORTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PoolingSupport getPoolingSupport()
    {
        return poolingSupport;
    }
}
