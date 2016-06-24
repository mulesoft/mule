/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.connection;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.module.extension.internal.runtime.config.ConnectionProviderObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectBuilderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

/**
 * A {@link ValueResolver} specialization for producing {@link ConnectionProvider}
 * instances through a {@link ConnectionProviderObjectBuilder}
 *
 * @since 4.0
 */
public class ConnectionProviderResolver implements ValueResolver<ConnectionProvider>
{

    private final ObjectBuilderValueResolver<ConnectionProvider> objectBuilder;

    /**
     * Creates a new instance
     *
     * @param objectBuilder an object builder to instantiate the {@link ConnectionProvider}
     */
    public ConnectionProviderResolver(ConnectionProviderObjectBuilder objectBuilder)
    {
        this.objectBuilder = new ObjectBuilderValueResolver<>(objectBuilder);
    }

    @Override
    public ConnectionProvider resolve(MuleEvent event) throws MuleException
    {
        return objectBuilder.resolve(event);
    }

    @Override
    public boolean isDynamic()
    {
        return objectBuilder.isDynamic();
    }
}
