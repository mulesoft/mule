/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import org.mule.extension.introspection.ExtensionModel;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hold the state related to registered {@link ExtensionModel extensionModels} and their instances.
 * <p/>
 * It also provides utility methods and caches to easily locate pieces of such state.
 *
 * @since 3.7.0
 */
final class ExtensionRegistry
{

    private final LoadingCache<ExtensionModel, ExtensionStateTracker> extensionStates = CacheBuilder.newBuilder().build(new CacheLoader<ExtensionModel, ExtensionStateTracker>()
    {
        @Override
        public ExtensionStateTracker load(ExtensionModel key) throws Exception
        {
            return new ExtensionStateTracker();
        }
    });

    private final Map<String, ExtensionModel> extensions = new ConcurrentHashMap<>();

    ExtensionRegistry()
    {
    }

    /**
     * Registers the given {@code extension}
     *
     * @param name      the registration name you want for the {@code extension}
     * @param extensionModel a {@link ExtensionModel}
     */
    void registerExtension(String name, ExtensionModel extensionModel)
    {
        extensions.put(name, extensionModel);
    }

    /**
     * @return an immutable view of the currently registered {@link ExtensionModel}
     */
    Set<ExtensionModel> getExtensions()
    {
        return ImmutableSet.copyOf(extensions.values());
    }

    /**
     * @param name the registration name of the {@link ExtensionModel} you want to test
     * @return {@code true} if an {@link ExtensionModel} is registered under {@code name}. {@code false} otherwise
     */
    boolean containsExtension(String name)
    {
        return extensions.containsKey(name);
    }

    /**
     * @param extensionModel a registered {@link ExtensionModel}
     * @return the {@link ExtensionStateTracker} object related to the given {@code extension}
     */
    ExtensionStateTracker getExtensionState(ExtensionModel extensionModel)
    {
        return extensionStates.getUnchecked(extensionModel);
    }

    /**
     * Returns a {@link Map} which keys are registrations keys and the values are the configuration instances
     * which are expired
     *
     * @return an immutable {@link Map}
     */
    Map<String, Object> getExpiredConfigs()
    {
        ImmutableMap.Builder<String, Object> expired = ImmutableMap.builder();
        extensionStates.asMap().values().stream().map(tracker -> tracker.getExpiredConfigs()).forEach(expired::putAll);

        return expired.build();
    }

}
