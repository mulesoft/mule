/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml.schema;

import org.mule.api.config.PoolingProfile;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ConnectionHandlingStrategy;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.api.connection.PoolingListener;
import org.mule.extension.api.introspection.ConnectionProviderModel;
import org.mule.util.ValueHolder;

final class SchemaBuilderUtils
{

    private SchemaBuilderUtils()
    {
    }

    public static PoolingSupport getPoolingDefinition(ConnectionProviderModel connectionProviderModel)
    {
        ConnectionProvider connectionProvider = connectionProviderModel.getConnectionProviderFactory().newInstance();
        ValueHolder<PoolingSupport> value = new ValueHolder<>();
        connectionProvider.getHandlingStrategy(new ConnectionHandlingStrategyFactory()
        {
            @Override
            public ConnectionHandlingStrategy supportsPooling(PoolingProfile defaultPoolingProfile)
            {
                value.set(PoolingSupport.SUPPORTED);
                return null;
            }

            @Override
            public ConnectionHandlingStrategy supportsPooling(PoolingProfile defaultPoolingProfile, PoolingListener poolingListener)
            {
                return supportsPooling(defaultPoolingProfile);
            }

            @Override
            public ConnectionHandlingStrategy requiresPooling(PoolingProfile defaultPoolingProfile, PoolingListener poolingListener)
            {
                return requiresPooling(defaultPoolingProfile);
            }

            @Override
            public ConnectionHandlingStrategy requiresPooling(PoolingProfile defaultPoolingProfile)
            {
                value.set(PoolingSupport.REQUIRED);
                return null;
            }

            @Override
            public ConnectionHandlingStrategy cached()
            {
                return none();
            }

            @Override
            public ConnectionHandlingStrategy none()
            {
                value.set(PoolingSupport.NOT_SUPPORTED);
                return null;
            }
        });

        return value.get();
    }
}
