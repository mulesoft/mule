/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.config;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.extension.introspection.ConfigurationModel;
import org.mule.api.extension.runtime.ConfigurationProvider;
import org.mule.api.extension.runtime.ConfigurationInstance;
import org.mule.module.extension.internal.runtime.DynamicConfigPolicy;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;

/**
 * Default implementation of {@link ConfigurationProviderFactory}
 *
 * @since 4.0
 */
public final class DefaultConfigurationProviderFactory implements ConfigurationProviderFactory
{

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> ConfigurationProvider<T> createDynamicConfigurationProvider(
            String name,
            ConfigurationModel configurationModel,
            ResolverSet resolverSet,
            DynamicConfigPolicy dynamicConfigPolicy) throws Exception
    {
        return new DynamicConfigurationProvider<>(name,
                                                  configurationModel,
                                                  resolverSet,
                                                  dynamicConfigPolicy.getExpirationPolicy());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> ConfigurationProvider<T> createStaticConfigurationProvider(
            String name,
            ConfigurationModel configurationModel,
            ResolverSet resolverSet,
            MuleContext muleContext) throws Exception
    {
        ConfigurationInstance<T> configuration;
        try
        {
            configuration = new ConfigurationInstanceFactory<T>(configurationModel, resolverSet).createConfiguration(name, getInitialiserEvent(muleContext));
        }
        catch (MuleException e)
        {
            throw new ConfigurationException(createStaticMessage(String.format("Could not create configuration '%s' for the '%s'",
                                                                               name, configurationModel.getExtensionModel().getName())), e);
        }

        return new StaticConfigurationProvider<>(name, configurationModel, configuration);
    }
}
