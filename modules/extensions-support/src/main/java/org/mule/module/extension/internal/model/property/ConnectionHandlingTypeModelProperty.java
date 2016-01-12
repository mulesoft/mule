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
import org.mule.module.extension.internal.introspection.utils.PoolingSupport;

/**
 * A immutable model property for {@link org.mule.extension.api.introspection.ConnectionProviderModel} which describes
 * the Connection Handling Type for a given ConnectionProvider.
 *
 * @since 4.0
 */
public final class ConnectionHandlingTypeModelProperty
{

    private boolean cached = false;
    private PoolingSupport poolingSupport = PoolingSupport.NOT_SUPPORTED;
    private boolean none = false;

    /**
     * A unique key that identifies this property type
     */
    public static final String KEY = ConnectionHandlingTypeModelProperty.class.getName();

    public ConnectionHandlingTypeModelProperty(ConnectionProvider connectionProvider)
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

    public boolean isCached()
    {
        return cached;
    }

    public boolean isNone()
    {
        return none;
    }

    public boolean isPooled()
    {
        return !poolingSupport.equals(PoolingSupport.NOT_SUPPORTED);
    }

    public PoolingSupport getPoolingSupport()
    {
        return poolingSupport;
    }
}
