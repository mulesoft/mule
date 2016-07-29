/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.config;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.time.TimeSupplier;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionProviderResolver;
import org.mule.runtime.module.extension.internal.runtime.DynamicConfigPolicy;
import org.mule.runtime.module.extension.internal.runtime.config.ConfigurationProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultConfigurationProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.ImplicitConnectionProviderValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import java.util.Optional;

import javax.inject.Inject;

/**
 * A {@link AbstractExtensionObjectFactory} which produces {@link ConfigurationProvider}
 * instances
 *
 * @since 4.0
 */
class ConfigurationProviderObjectFactory extends AbstractExtensionObjectFactory<ConfigurationProvider<Object>> implements ObjectFactory<ConfigurationProvider<Object>>
{

    private final String name;
    private final RuntimeConfigurationModel configurationModel;
    private final ConfigurationProviderFactory configurationProviderFactory = new DefaultConfigurationProviderFactory();
    private final MuleContext muleContext;

    private DynamicConfigPolicy dynamicConfigPolicy;
    private Optional<ValueResolver<ConnectionProvider>> connectionProviderResolver = empty();
    private ConfigurationProvider<Object> instance;
    private boolean requiresConnection = false;

    @Inject
    private TimeSupplier timeSupplier;


    ConfigurationProviderObjectFactory(String name,
                                       RuntimeConfigurationModel configurationModel,
                                       MuleContext muleContext)
    {
        this.name = name;
        this.configurationModel = configurationModel;
        this.muleContext = muleContext;
    }

    @Override
    public ConfigurationProvider<Object> getObject() throws Exception
    {
        if (instance == null)
        {
            instance = createInnerInstance();
        }
        return instance;
    }

    private ConfigurationProvider<Object> createInnerInstance() throws ConfigurationException
    {
        ResolverSet resolverSet = getParametersAsResolverSet(configurationModel);
        final ValueResolver<ConnectionProvider> connectionProviderResolver = getConnectionProviderResolver();

        ConfigurationProvider<Object> configurationProvider;
        try
        {
            if (resolverSet.isDynamic() || connectionProviderResolver.isDynamic())
            {
                configurationProvider = configurationProviderFactory.createDynamicConfigurationProvider(
                        name,
                        configurationModel,
                        resolverSet,
                        connectionProviderResolver,
                        getDynamicConfigPolicy());
            }
            else
            {
                configurationProvider = configurationProviderFactory.createStaticConfigurationProvider(
                        name,
                        configurationModel,
                        resolverSet,
                        connectionProviderResolver,
                        muleContext);
            }

            muleContext.getInjector().inject(configurationProvider);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return configurationProvider;
    }

    private DynamicConfigPolicy getDynamicConfigPolicy()
    {
        if (dynamicConfigPolicy == null)
        {
            dynamicConfigPolicy = DynamicConfigPolicy.getDefault(timeSupplier);
        }

        return dynamicConfigPolicy;
    }

    private ValueResolver<ConnectionProvider> getConnectionProviderResolver()
    {
        return connectionProviderResolver.orElseGet(() -> {
            if (requiresConnection)
            {
                return new ImplicitConnectionProviderValueResolver(name, configurationModel);
            }
            return new StaticValueResolver<>(null);
        });
    }

    public void setDynamicConfigPolicy(DynamicConfigPolicy dynamicConfigPolicy)
    {
        this.dynamicConfigPolicy = dynamicConfigPolicy;
    }

    public void setConnectionProviderResolver(ConnectionProviderResolver connectionProviderResolver)
    {
        this.connectionProviderResolver = ofNullable(connectionProviderResolver);
    }

    public void setRequiresConnection(boolean requiresConnection)
    {
        this.requiresConnection = requiresConnection;
    }
}
