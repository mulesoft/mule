/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;
import org.mule.extension.runtime.ConfigurationInstanceProvider;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds state regarding the use that the platform is doing of a
 * certain {@link Extension}
 *
 * @since 3.7.0
 */
final class ExtensionStateTracker
{

    private final LoadingCache<Configuration, ConfigurationStateTracker> configurationsStates = CacheBuilder.newBuilder()
            .build(new CacheLoader<Configuration, ConfigurationStateTracker>()
            {
                @Override
                public ConfigurationStateTracker load(Configuration configuration) throws Exception
                {
                    return new ConfigurationStateTracker();
                }
            });

    private final Map<String, ConfigurationInstanceProvider<?>> configurationInstanceProviders = new ConcurrentHashMap<>();

    /**
     * Registers than when a configuration instance with the given {@code providerName} is requested,
     * {@code configurationInstanceProvider} should be used to fulfil that request
     *
     * @param providerName                  the name under which the {@code configurationInstanceProvider} will be registered
     * @param configurationInstanceProvider the {@link ConfigurationInstanceProvider} to use
     * @param <C>                           the generic type of the configuration instances that will be provisioned by {@code configurationInstanceProvider}
     */
    <C> void registerConfigurationInstanceProvider(String providerName, ConfigurationInstanceProvider<C> configurationInstanceProvider)
    {
        if (configurationInstanceProviders.containsKey(providerName))
        {
            throw new IllegalStateException(String.format("A %s is already registered for the name '%s'",
                                                          ConfigurationInstanceProvider.class.getSimpleName(), providerName));
        }

        configurationInstanceProviders.put(providerName, configurationInstanceProvider);
    }

    <C> ConfigurationInstanceProvider<C> getConfigurationInstanceProvider(String configurationInstanceProviderName)
    {
        return (ConfigurationInstanceProvider<C>) configurationInstanceProviders.get(configurationInstanceProviderName);
    }

    List<ConfigurationInstanceProvider<?>> getConfigurationInstanceProviders()
    {
        return ImmutableList.copyOf(configurationInstanceProviders.values());
    }

    <C> void registerConfigurationInstance(Configuration configuration, String instanceName, C configurationInstance)
    {
        getStateTracker(configuration).registerInstance(instanceName, configurationInstance);
    }

    private ConfigurationStateTracker getStateTracker(Configuration configuration)
    {
        return configurationsStates.getUnchecked(configuration);
    }
}
