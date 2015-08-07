/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.registry.MuleRegistry;
import org.mule.extension.ExtensionManager;
import org.mule.extension.introspection.Extension;
import org.mule.extension.runtime.ConfigurationInstanceProvider;

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

    /**
     * Correlates instances of {@link ConfigurationInstanceProvider} by the key in which they were defined
     * through the {@link ExtensionManager#registerConfigurationInstanceProvider(Extension, String, ConfigurationInstanceProvider)}
     * method.
     */
    private final Map<String, ConfigurationInstanceProvider<?>> configurationInstanceProviders = new ConcurrentHashMap<>();

    /**
     * Correlates a configuration instance to the key on which it was registered on the
     * {@link MuleRegistry}
     */
    private final Map<String, Object> configurationInstances = new ConcurrentHashMap<>();

    /**
     * Registers the {@code configurationInstanceProvider} than shoudl be used when a configuration instance
     * for the given {@code key} is requested
     *
     * @param key                           the name under which the {@code configurationInstanceProvider} will be registered
     * @param configurationInstanceProvider the {@link ConfigurationInstanceProvider} to use
     * @param <C>                           the generic type of the configuration instances that will be provisioned by {@code configurationInstanceProvider}
     */
    <C> void registerConfigurationInstanceProvider(String key, ConfigurationInstanceProvider<C> configurationInstanceProvider)
    {
        idempotentPut(configurationInstanceProviders, key, configurationInstanceProvider);
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
        return (ConfigurationInstanceProvider<C>) configurationInstanceProviders.get(configurationInstanceProviderName);
    }

    /**
     * Returns an immutable {@link List} with all the {@link ConfigurationInstanceProvider} instances
     * that were registered through {@link #registerConfigurationInstanceProvider(String, ConfigurationInstanceProvider)}
     *
     * @return a immutable {@link List} with the registered {@link ConfigurationInstanceProvider}. May be empty but will never be {@code null}
     */
    List<ConfigurationInstanceProvider<?>> getConfigurationInstanceProviders()
    {
        return ImmutableList.copyOf(configurationInstanceProviders.values());
    }

    /**
     * Registers the creation of a new configuration instance
     *
     * @param instanceKey           the key under which the instance was registered in the {@link MuleRegistry}
     * @param configurationInstance the configuration instance. Cannot be {@code null}
     * @param <C>                   the generic type for the {@code configurationInstance}
     */
    <C> void registerConfigurationInstance(String instanceKey, C configurationInstance)
    {
        idempotentPut(configurationInstances, instanceKey, configurationInstance);
    }

    private <K, V> void idempotentPut(Map<K, V> map, K key, V value)
    {
        checkArgument(value != null, "value cannot be null");
        if (map.containsKey(key))
        {
            throw new IllegalStateException(String.format("A %s is already registered for the name '%s'", value.getClass().getSimpleName(), key));
        }

        map.put(key, value);
    }
}
