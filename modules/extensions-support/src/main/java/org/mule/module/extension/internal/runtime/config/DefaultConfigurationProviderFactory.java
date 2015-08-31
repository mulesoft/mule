/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.config;

import static org.mule.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import org.mule.api.MuleContext;
import org.mule.extension.ExtensionManager;
import org.mule.extension.introspection.ConfigurationModel;
import org.mule.extension.introspection.ExtensionModel;
import org.mule.extension.runtime.ConfigurationProvider;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
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
            ExtensionModel extensionModel,
            ConfigurationModel configurationModel,
            ResolverSet resolverSet,
            ExtensionManagerAdapter extensionManager,
            DynamicConfigPolicy dynamicConfigPolicy) throws Exception
    {
        ConfigurationObjectBuilder configurationObjectBuilder = new ConfigurationObjectBuilder(configurationModel, resolverSet);
        ConfigurationProvider<T> configurationProvider = new DynamicConfigurationProvider<>(name,
                                                                                            extensionModel,
                                                                                            configurationModel,
                                                                                            extensionManager,
                                                                                            configurationObjectBuilder,
                                                                                            resolverSet,
                                                                                            dynamicConfigPolicy.getExpirationPolicy());

        register(extensionModel, configurationProvider, extensionManager);

        return configurationProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> ConfigurationProvider<T> createStaticConfigurationProvider(
            String name,
            ExtensionModel extensionModel,
            ConfigurationModel configurationModel,
            ResolverSet resolverSet,
            MuleContext muleContext,
            ExtensionManagerAdapter extensionManager) throws Exception
    {
        ConfigurationObjectBuilder configurationObjectBuilder = new ConfigurationObjectBuilder(configurationModel, resolverSet);
        ConfigurationProvider<T> configurationProvider;


        T configuration = (T) configurationObjectBuilder.build(getInitialiserEvent(muleContext));
        configurationProvider = new StaticConfigurationProvider<>(name, configurationModel, configuration);
        register(extensionModel, configurationProvider, extensionManager);
        extensionManager.registerConfiguration(extensionModel, name, configuration);

        return configurationProvider;
    }

    private <T> void register(ExtensionModel extensionModel, ConfigurationProvider<T> configurationProvider, ExtensionManager extensionManager)
    {
        extensionManager.registerConfigurationProvider(extensionModel, configurationProvider);
    }
}
