/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import org.mule.extension.exception.NoSuchExtensionException;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Operation;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.util.CollectionUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.Predicate;

/**
 * Hold the state related to registered {@link Extension}s and their instances.
 * <p/>
 * It also provides utility methods and caches to easily locate pieces of such state.
 *
 * @since 3.7.0
 */
final class ExtensionRegistry
{

    private final LoadingCache<Extension, ExtensionStateTracker> extensionStates = CacheBuilder.newBuilder().build(new CacheLoader<Extension, ExtensionStateTracker>()
    {
        @Override
        public ExtensionStateTracker load(Extension key) throws Exception
        {
            return new ExtensionStateTracker();
        }
    });

    private final Map<String, Extension> extensions = new ConcurrentHashMap<>();
    private final Map<Configuration, Extension> configurationToExtensions = new ConcurrentHashMap<>();
    private final Map<Operation, Extension> operationToExtension = new ConcurrentHashMap<>();
    private final Map<Class<?>, Set<Extension>> capabilityToExtension = new ConcurrentHashMap<>();

    ExtensionRegistry()
    {
    }

    /**
     * Registers the given {@code extension}
     *
     * @param name      the registration name you want for the {@code extension}
     * @param extension a {@link Extension}
     */
    void registerExtension(String name, Extension extension)
    {
        extensions.put(name, extension);
    }

    /**
     * @return an immutable view of the currently registered {@link Extension}
     */
    Set<Extension> getExtensions()
    {
        return ImmutableSet.copyOf(extensions.values());
    }

    /**
     * @param name the registration name of the {@link Extension} you want to test
     * @return {@code true} if an {@link Extension} is registered under {@code name}. {@code false} otherwise
     */
    boolean containsExtension(String name)
    {
        return extensions.containsKey(name);
    }

    /**
     * @param name the registration of the extension you want
     * @return the registered {@link Extension} or {@code null} if nothing was registered with that {@code name}
     */
    Extension getExtension(String name)
    {
        return extensions.get(name);
    }

    /**
     * @param configuration a valid {@link Configuration} model
     * @return the {@link Extension} that owns the given {@code configuration}
     */
    Extension getExtension(final Configuration configuration)
    {
        Extension extension = lookupInCache(configurationToExtensions, configuration, new Predicate()
        {
            @Override
            public boolean evaluate(Object object)
            {
                Extension extension = (Extension) object;
                return extension.getConfiguration(configuration.getName()) == configuration;
            }
        });

        if (extension == null)
        {
            throw new NoSuchExtensionException("Could not find a registered extension which contains the configuration " + configuration.getName());
        }

        return extension;
    }

    /**
     * @param operation a valid {@link Operation} model
     * @return the {@link Operation} that owns the given {@code operation}
     */
    Extension getExtension(final Operation operation)
    {
        Extension extension = lookupInCache(operationToExtension, operation, new Predicate()
        {
            @Override
            public boolean evaluate(Object object)
            {
                Extension extension = (Extension) object;
                return extension.getOperation(operation.getName()) == operation;
            }
        });

        if (extension == null)
        {
            throw new NoSuchExtensionException("Could not find a registered extension which contains the operation " + operation.getName());
        }

        return extension;
    }

    /**
     * @param extension a registered {@link Extension}
     * @return the {@link ExtensionStateTracker} object related to the given {@code extension}
     */
    ExtensionStateTracker getExtensionState(Extension extension)
    {
        return extensionStates.getUnchecked(extension);
    }

    /**
     * @param configuration a {@link Configuration} owned by a registered {@link Extension}
     * @return the {@link ExtensionStateTracker} object related to the given {@code configuration}
     */
    ExtensionStateTracker getExtensionState(Configuration configuration)
    {
        return getExtensionState(getExtension(configuration));
    }

    Map<String, ConfigurationInstanceProvider> getConfigurationInstanceProviders()
    {
        ImmutableMap.Builder<String, ConfigurationInstanceProvider> providers = ImmutableMap.builder();
        for (Extension extension : extensions.values())
        {
            providers.putAll(getConfigurationInstanceProviders(extension));
        }

        return providers.build();
    }

    Map<String, ConfigurationInstanceProvider> getConfigurationInstanceProviders(Extension extension)
    {
        ImmutableMap.Builder<String, ConfigurationInstanceProvider> providers = ImmutableMap.builder();
        for (Configuration configuration : extension.getConfigurations())
        {
            providers.putAll(getExtensionState(configuration).getConfigurationInstanceProviders(configuration));
        }

        return providers.build();
    }

    /**
     * @param operation a {@link Operation} owned by a registered {@link Extension}
     * @return the {@link ExtensionStateTracker} object related to the given {@code operation}
     */
    ExtensionStateTracker getExtensionState(Operation operation)
    {
        return getExtensionState(getExtension(operation));
    }

    /**
     * @param capabilityType the {@link Class} of a capability
     * @param <C>            the capability type
     * @return an immutable view of all registered {@link Extension} which have the given {@code capabilityType}
     */
    <C> Set<Extension> getExtensionsCapableOf(Class<C> capabilityType)
    {
        Set<Extension> cachedCapables = capabilityToExtension.get(capabilityType);
        if (CollectionUtils.isEmpty(cachedCapables))
        {
            ImmutableSet.Builder<Extension> capables = ImmutableSet.builder();
            for (Extension extension : getExtensions())
            {
                if (extension.isCapableOf(capabilityType))
                {
                    capables.add(extension);
                }
            }

            cachedCapables = capables.build();
            capabilityToExtension.put(capabilityType, cachedCapables);
        }

        return cachedCapables;
    }

    private <K, V> V lookupInCache(Map<K, V> cache, final K key, Predicate predicate)
    {
        V value = cache.get(key);
        if (value == null)
        {
            value = (V) CollectionUtils.find(extensions.values(), predicate);

            if (value != null)
            {
                cache.put(key, value);
            }
        }

        return value;
    }
}
