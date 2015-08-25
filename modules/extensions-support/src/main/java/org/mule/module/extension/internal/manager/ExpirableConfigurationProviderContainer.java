/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.mule.util.MapUtils.idempotentPut;
import org.mule.extension.runtime.ConfigurationProvider;
import org.mule.module.extension.internal.runtime.ExpirableContainer;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decorates a {@link #configurationProvider} with extra
 * management information like the configuration instances it has generated and their
 * registration keys.
 * <p/>
 * It also implements the {@link ExpirableContainer} interface which means that it can
 * determine which configuration instances should be expired (only as long as the
 * {@link #configurationProvider} itself implements the same interface
 *
 * @since 4.0
 */
final class ExpirableConfigurationProviderContainer implements ExpirableContainer<Object>
{

    private final ConfigurationProvider<?> configurationProvider;
    private final Map<String, Object> configurations = new ConcurrentHashMap<>();

    /**
     * Creates a new instance which contains the given {@code configurationProvider}
     *
     * @param configurationProvider the provider to be decorated
     */
    ExpirableConfigurationProviderContainer(ConfigurationProvider<?> configurationProvider)
    {
        this.configurationProvider = configurationProvider;
    }

    ConfigurationProvider<?> getConfigurationProvider()
    {
        return configurationProvider;
    }

    void addConfiguration(String registrationName, Object configuration)
    {
        idempotentPut(configurations, registrationName, configuration);
    }

    /**
     * if {@link #configurationProvider} implements the {@link ExpirableContainer} interface
     * it returns a immutable copy of delegating this method into it. Otherwise, it just returns an empty map
     *
     * @return a immutable {@link Map}
     */
    @Override
    public Map<String, Object> getExpired()
    {
        if (configurationProvider instanceof ExpirableContainer)
        {
            return ImmutableMap.copyOf(((ExpirableContainer<Object>) configurationProvider).getExpired());
        }

        return ImmutableMap.of();
    }
}
