/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.describer;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.module.extension.internal.util.IntrospectionUtils.checkInstantiable;
import org.mule.api.MuleRuntimeException;
import org.mule.api.connection.ConnectionProvider;
import org.mule.module.extension.internal.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.ConnectionProviderFactory;

/**
 * Creates instances of {@link ConnectionProvider} based on a {@link #providerClass}
 * * @param <Config>     the generic type for the configuration objects that the created {@link ConnectionProvider providers} accept
 *
 * @param <Connection> the generic type for the connections that the created  {@link ConnectionProvider providers} produce
 * @since 4.0
 */
final class DefaultConnectionProviderFactory<Config, Connection> implements ConnectionProviderFactory<Config, Connection>
{

    private final Class<? extends ConnectionProvider> providerClass;

    /**
     * Creates a new instance which creates {@link ConnectionProvider} instances of the given
     * {@code providerClass}
     *
     * @param providerClass the {@link Class} of the created {@link ConnectionProvider providers}
     * @throws IllegalModelDefinitionException if {@code providerClass} doesn't implement the {@link ConnectionProvider} interface
     * @throws IllegalArgumentException        if {@code providerClass} is not an instantiable type
     */
    DefaultConnectionProviderFactory(Class<?> providerClass)
    {
        if (!ConnectionProvider.class.isAssignableFrom(providerClass))
        {
            throw new IllegalConnectionProviderModelDefinitionException(String.format(
                    "Class '%s' was specified as a connection provider but it doesn't implement the '%s' interface",
                    providerClass.getName(), ConnectionProvider.class.getName()));
        }

        checkInstantiable(providerClass);
        this.providerClass = (Class<? extends ConnectionProvider>) providerClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionProvider<Config, Connection> newInstance()
    {
        try
        {
            return (ConnectionProvider) providerClass.newInstance();
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(createStaticMessage("Could not create connection provider of type " + providerClass.getName()), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends ConnectionProvider> getObjectType()
    {
        return providerClass;
    }
}
