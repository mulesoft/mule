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
import org.mule.extension.introspection.Extension;
import org.mule.extension.runtime.ConfigurationInstanceProvider;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Holds state regarding the use that the platform is doing of a
 * certain {@link Extension}
 *
 * @since 3.7.0
 */
final class ExtensionStateTracker
{

    /**
     * Correlates instances of {@link ConfigurationInstanceProvider} by the key in which they were defined
     * through the {@link ExtensionManager#registerConfigurationInstanceProvider(Extension, String, ConfigurationInstanceProvider)}
     * method.
     */
    private final Map<String, ExpirableConfigurationInstanceProviderContainer> configurationInstanceProviders = new ConcurrentHashMap<>();

    /**
     * Registers the {@code configurationInstanceProvider} than should be used when a configuration instance
     * for the given {@code key} is requested
     *
     * @param key                           the name under which the {@code configurationInstanceProvider} will be registered
     * @param configurationInstanceProvider the {@link ConfigurationInstanceProvider} to use
     * @param <C>                           the generic type of the configuration instances that will be provisioned by {@code configurationInstanceProvider}
     */
    <C> void registerConfigurationInstanceProvider(String key, ConfigurationInstanceProvider<C> configurationInstanceProvider)
    {
        idempotentPut(configurationInstanceProviders, key, new ExpirableConfigurationInstanceProviderContainer(configurationInstanceProvider));
    }

    /**
     * Returns the {@link ConfigurationInstanceProvider} that was registered under {@code configurationInstanceProviderName}
     * through the {@link #registerConfigurationInstanceProvider(String, ConfigurationInstanceProvider)} method
     *
     * @param configurationInstanceProviderName the registration name of the {@link ConfigurationInstanceProvider} you're looking for
     * @param <C>                               the generic type of the {@link ConfigurationInstanceProvider}
     * @return a registered {@link ConfigurationInstanceProvider} or {@code null} if no such provider was registered
     */
    <C> ConfigurationInstanceProvider<C> getConfigurationInstanceProvider(String configurationInstanceProviderName)
    {
        return (ConfigurationInstanceProvider<C>) getWrapper(configurationInstanceProviderName).getConfigurationInstanceProvider();
    }

    /**
     * Returns an immutable {@link List} with all the {@link ConfigurationInstanceProvider} instances
     * that were registered through {@link #registerConfigurationInstanceProvider(String, ConfigurationInstanceProvider)}
     *
     * @return a immutable {@link List} with the registered {@link ConfigurationInstanceProvider}. May be empty but will never be {@code null}
     */
    List<ConfigurationInstanceProvider<?>> getConfigurationInstanceProviders()
    {
        return ImmutableList.copyOf(
                configurationInstanceProviders.values()
                        .stream()
                        .map(wrapper -> wrapper.getConfigurationInstanceProvider())
                        .collect(Collectors.toList()));
    }

    /**
     * Registers the creation of a new configuration instance
     *
     * @param providerName          the name of a registered {@link ConfigurationInstanceProvider}
     * @param registrationName      the name on which the {@code configurationInstance} has been registered on the {@link MuleRegistry}
     * @param configurationInstance the configuration instance. Cannot be {@code null}
     * @param <C>                   the generic type for the {@code configurationInstance}
     */
    <C> void registerConfigurationInstance(String providerName, String registrationName, C configurationInstance)
    {
        ExpirableConfigurationInstanceProviderContainer wrapper = getWrapper(providerName);
        wrapper.addConfigurationInstance(registrationName, configurationInstance);
    }

    private ExpirableConfigurationInstanceProviderContainer getWrapper(String configurationInstanceProviderName)
    {
        ExpirableConfigurationInstanceProviderContainer wrapper = configurationInstanceProviders.get(configurationInstanceProviderName);
        if (wrapper == null)
        {
            throw new IllegalArgumentException(String.format("No %s was registered with name %s",
                                                             ConfigurationInstanceProvider.class.getName(),
                                                             configurationInstanceProviderName));
        }
        return wrapper;
    }


    Map<String, Object> getExpiredConfigInstances()
    {
        ImmutableMap.Builder<String, Object> expired = ImmutableMap.builder();
        configurationInstanceProviders.values().stream().map(wrapper -> wrapper.getExpired()).forEach(expired::putAll);

        return expired.build();
    }
}
