/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.mule.util.MapUtils.idempotentPut;
import org.mule.api.registry.MuleRegistry;
import org.mule.extension.ExtensionManager;
import org.mule.extension.introspection.ExtensionModel;
import org.mule.extension.runtime.ConfigurationProvider;
import org.mule.util.collection.ImmutableListCollector;

import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds state regarding the use that the platform is doing of a
 * certain {@link ExtensionModel}
 *
 * @since 3.7.0
 */
final class ExtensionStateTracker
{

    /**
     * Correlates instances of {@link ConfigurationProvider} by the key in which they were defined
     * through the {@link ExtensionManager#registerConfigurationProvider(ExtensionModel, ConfigurationProvider)}
     * method.
     */
    private final Map<String, ExpirableConfigurationProviderContainer> configurationProviders = new ConcurrentHashMap<>();

    /**
     * Registers the {@code configurationProvider} than should be used when a configuration instance
     * for the given {@code key} is requested
     *
     * @param key                   the name under which the {@code configurationProvider} will be registered
     * @param configurationProvider the {@link ConfigurationProvider} to use
     * @param <C>                   the generic type of the configuration instances that will be provisioned by {@code configurationProvider}
     */
    <C> void registerConfigurationProvider(String key, ConfigurationProvider<C> configurationProvider)
    {
        idempotentPut(configurationProviders, key, new ExpirableConfigurationProviderContainer(configurationProvider));
    }

    /**
     * Returns the {@link ConfigurationProvider} that was registered under {@code configurationProviderName}
     * through the {@link #registerConfigurationProvider(String, ConfigurationProvider)} method
     *
     * @param configurationProviderName the registration name of the {@link ConfigurationProvider} you're looking for
     * @param <C>                       the generic type of the {@link ConfigurationProvider}
     * @return a registered {@link ConfigurationProvider} or {@code null} if no such provider was registered
     */
    <C> ConfigurationProvider<C> getConfigurationProvider(String configurationProviderName)
    {
        return (ConfigurationProvider<C>) getWrapper(configurationProviderName).getConfigurationProvider();
    }

    /**
     * Returns an immutable {@link List} with all the {@link ConfigurationProvider} instances
     * that were registered through {@link #registerConfigurationProvider(String, ConfigurationProvider)}
     *
     * @return a immutable {@link List} with the registered {@link ConfigurationProvider}. May be empty but will never be {@code null}
     */
    List<ConfigurationProvider<?>> getConfigurationProviders()
    {
        return configurationProviders.values()
                .stream()
                .map(wrapper -> wrapper.getConfigurationProvider())
                .collect(new ImmutableListCollector<>());
    }

    /**
     * Registers the creation of a new configuration instance
     *
     * @param providerName     the name of a registered {@link ConfigurationProvider}
     * @param registrationName the name on which the {@code configuration} has been registered on the {@link MuleRegistry}
     * @param configuration    the configuration instance. Cannot be {@code null}
     * @param <C>              the generic type for the {@code configuration}
     */
    <C> void registerConfiguration(String providerName, String registrationName, C configuration)
    {
        ExpirableConfigurationProviderContainer wrapper = getWrapper(providerName);
        wrapper.addConfiguration(registrationName, configuration);
    }

    private ExpirableConfigurationProviderContainer getWrapper(String configurationProviderName)
    {
        ExpirableConfigurationProviderContainer wrapper = configurationProviders.get(configurationProviderName);
        if (wrapper == null)
        {
            throw new IllegalArgumentException(String.format("No %s was registered with name %s",
                                                             ConfigurationProvider.class.getName(),
                                                             configurationProviderName));
        }
        return wrapper;
    }


    Map<String, Object> getExpiredConfigs()
    {
        ImmutableMap.Builder<String, Object> expired = ImmutableMap.builder();
        configurationProviders.values().stream().map(wrapper -> wrapper.getExpired()).forEach(expired::putAll);

        return expired.build();
    }
}
