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
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.module.extension.internal.runtime.DynamicConfigPolicy;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;

/**
 * Default implementation of {@link ConfigurationInstanceProviderFactory}
 *
 * @since 4.0
 */
public final class DefaultConfigurationInstanceProviderFactory implements ConfigurationInstanceProviderFactory
{

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> ConfigurationInstanceProvider<T> createDynamicConfigurationInstanceProvider(
            String name,
            Extension extension,
            Configuration configuration,
            ResolverSet resolverSet,
            ExtensionManagerAdapter extensionManager,
            DynamicConfigPolicy dynamicConfigPolicy) throws Exception
    {
        ConfigurationObjectBuilder configurationObjectBuilder = new ConfigurationObjectBuilder(configuration, resolverSet);
        ConfigurationInstanceProvider<T> configurationInstanceProvider = new DynamicConfigurationInstanceProvider<>(name,
                                                                                                                    extension,
                                                                                                                    extensionManager,
                                                                                                                    configurationObjectBuilder,
                                                                                                                    resolverSet,
                                                                                                                    dynamicConfigPolicy.getExpirationPolicy());

        register(extension, name, configurationInstanceProvider, extensionManager);

        return configurationInstanceProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> ConfigurationInstanceProvider<T> createStaticConfigurationInstanceProvider(
            String name,
            Extension extension,
            Configuration configuration,
            ResolverSet resolverSet,
            MuleContext muleContext,
            ExtensionManagerAdapter extensionManager) throws Exception
    {
        ConfigurationObjectBuilder configurationObjectBuilder = new ConfigurationObjectBuilder(configuration, resolverSet);
        ConfigurationInstanceProvider<T> configurationInstanceProvider;


        T configurationInstance = (T) configurationObjectBuilder.build(getInitialiserEvent(muleContext));
        configurationInstanceProvider = new StaticConfigurationInstanceProvider<>(configurationInstance);
        register(extension, name, configurationInstanceProvider, extensionManager);
        extensionManager.registerConfigurationInstance(extension, name, configurationInstance);

        return configurationInstanceProvider;
    }

    private <T> void register(Extension extension, String name, ConfigurationInstanceProvider<T> configurationInstanceProvider, ExtensionManager extensionManager)
    {
        extensionManager.registerConfigurationInstanceProvider(extension, name, configurationInstanceProvider);
    }
}
