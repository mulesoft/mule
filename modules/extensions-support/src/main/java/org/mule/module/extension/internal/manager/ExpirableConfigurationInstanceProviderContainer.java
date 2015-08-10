/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.mule.util.MapUtils.idempotentPut;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.module.extension.internal.runtime.ExpirableContainer;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decorates a {@link #configurationInstanceProvider} with extra
 * management information like the configuration instances it has generated and their
 * registration keys.
 * <p/>
 * It also implements the {@link ExpirableContainer} interface which means that it can
 * determine which configuration instances should be expired (only as long as the
 * {@link #configurationInstanceProvider} itself implements the same interface
 *
 * @since 4.0
 */
final class ExpirableConfigurationInstanceProviderContainer implements ExpirableContainer<Object>
{

    private final ConfigurationInstanceProvider<?> configurationInstanceProvider;
    private final Map<String, Object> configurationInstances = new ConcurrentHashMap<>();

    /**
     * Creates a new instance which contains the given {@code configurationInstanceProvider}
     *
     * @param configurationInstanceProvider the provider to be decorated
     */
    ExpirableConfigurationInstanceProviderContainer(ConfigurationInstanceProvider<?> configurationInstanceProvider)
    {
        this.configurationInstanceProvider = configurationInstanceProvider;
    }

    ConfigurationInstanceProvider<?> getConfigurationInstanceProvider()
    {
        return configurationInstanceProvider;
    }

    void addConfigurationInstance(String registrationName, Object configurationInstance)
    {
        idempotentPut(configurationInstances, registrationName, configurationInstance);
    }

    /**
     * if {@link #configurationInstanceProvider} implements the {@link ExpirableContainer} interface
     * it returns a immutable copy of delegating this method into it. Otherwise, it just returns an empty map
     *
     * @return a immutable {@link Map}
     */
    @Override
    public Map<String, Object> getExpired()
    {
        if (configurationInstanceProvider instanceof ExpirableContainer)
        {
            return ImmutableMap.copyOf(((ExpirableContainer<Object>) configurationInstanceProvider).getExpired());
        }

        return ImmutableMap.of();
    }
}
