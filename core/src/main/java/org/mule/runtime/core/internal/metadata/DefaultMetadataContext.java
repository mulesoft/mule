/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.metadata;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;

import java.util.Optional;

/**
 * Default immutable implementation of {@link MetadataContext}, it provides access to the extension configuration and
 * connection in the metadata fetch invocation.
 *
 * @since 4.0
 */
public class DefaultMetadataContext implements MetadataContext
{

    private final ConfigurationInstance<?> configInstance;
    private final ConnectionManager connectionManager;
    private final MetadataCache cache;

    /**
     * Retrieves the configuration for the related component
     *
     * @param configInstance    instance of the configuration of a component
     * @param connectionManager {@link ConnectionManager} which is able to find a connection for the component using the {@param configInstance}
     * @param cache             instance of the {@link MetadataCache} for this context
     */
    public DefaultMetadataContext(ConfigurationInstance<Object> configInstance, ConnectionManager connectionManager, MetadataCache cache)
    {
        this.configInstance = configInstance;
        this.connectionManager = connectionManager;
        this.cache = cache;
    }

    /**
     * @param <C> Configuration type
     * @return The instance of the configuration of a component
     */
    @Override
    public <C> C getConfig()
    {
        return (C) configInstance.getValue();
    }

    /**
     * Retrieves the connection for the related component and configuration
     *
     * @param <C> Connection type
     * @return A connection instance of {@param <C>} type for the component. If the related configuration does not
     * require a connection {@link Optional#empty()} will be returned
     * @throws ConnectionException when no valid connection is found for the related component and configuration
     */
    @Override
    public <C> Optional<C> getConnection() throws ConnectionException
    {
        if (!configInstance.getConnectionProvider().isPresent())
        {
            return Optional.empty();
        }

        return Optional.of((C) connectionManager.getConnection(configInstance.getValue()).getConnection());
    }

    @Override
    public MetadataCache getCache()
    {
        return cache;
    }
}
