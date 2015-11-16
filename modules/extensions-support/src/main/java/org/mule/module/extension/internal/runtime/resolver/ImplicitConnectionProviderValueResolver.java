/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.extension.api.connection.ConnectionProvider;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.module.extension.internal.runtime.config.DefaultImplicitConnectionProviderFactory;
import org.mule.module.extension.internal.runtime.config.ImplicitConnectionProviderFactory;

import java.util.function.Function;

/**
 * Uses a {@link ImplicitConnectionProviderFactory} to create an implicit
 * {@link ConnectionProvider}.
 * <p>
 * This is a static {@link ValueResolver}. The {@link ConnectionProvider} is created
 * the first time the {@link #resolve(MuleEvent)} method is invoked on {@code this} instance.
 * Subsequent invokations will return the same instance.
 * <p>
 * This class is thread-safe
 *
 * @since 4.0
 */
public final class ImplicitConnectionProviderValueResolver implements ValueResolver<ConnectionProvider>
{

    private ConnectionProvider connectionProvider = null;
    private Function<MuleEvent, ConnectionProvider> delegate;

    public ImplicitConnectionProviderValueResolver(String name, ExtensionModel extensionModel)
    {
        delegate = event -> {
            synchronized (this)
            {
                if (connectionProvider == null)
                {
                    connectionProvider = new DefaultImplicitConnectionProviderFactory().createImplicitConnectionProvider(name, extensionModel, event);
                    delegate = nextEvent -> connectionProvider;
                }
                return connectionProvider;
            }
        };
    }

    @Override
    public ConnectionProvider resolve(MuleEvent event) throws MuleException
    {
        return delegate.apply(event);
    }


    /**
     * @return {@code false}
     */
    @Override
    public boolean isDynamic()
    {
        return false;
    }
}
