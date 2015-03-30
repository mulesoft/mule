/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import static org.mule.util.Preconditions.checkState;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Operation;
import org.mule.extension.runtime.OperationExecutor;
import org.mule.util.CollectionUtils;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import org.apache.commons.collections.Predicate;

/**
 * A utility class to track state related to registered instances
 * which are realisations of {@link Configuration} models owned
 * by registered {@link Extension}s
 *
 * @since 3.7.0
 */
final class ConfigurationsStateTracker
{

    private final Multimap<Configuration, ConfigurationInstanceWrapper<?>> configurationInstances =
            Multimaps.synchronizedSetMultimap(LinkedHashMultimap.<Configuration, ConfigurationInstanceWrapper<?>>create());

    <C> void registerInstance(Configuration configuration, final String instanceName, final C configurationInstance)
    {
        configurationInstances.put(configuration, new ConfigurationInstanceWrapper<>(instanceName, configurationInstance));
    }

    /**
     * Returns an {@link OperationExecutor} that was previously registered
     * through {@link #registerOperationExecutor(Operation, Object, OperationExecutor)}
     *
     * @param operation             a {@link Operation} model
     * @param configurationInstance a previously registered configuration instance
     * @param <C>                   the type of the configuration instance
     * @return a {@link OperationExecutor}
     */
    <C> OperationExecutor getOperationExecutor(Operation operation, C configurationInstance)
    {
        ConfigurationInstanceWrapper<C> wrapper = locateConfigurationInstanceWrapper(configurationInstance);
        return wrapper.getOperationExecutor(operation);
    }

    /**
     * Registers a {@link OperationExecutor} for the {@code operation}|{@code configurationInstance}
     * pair.
     *
     * @param operation             a {@link Operation} model
     * @param configurationInstance a previously registered configuration instance
     * @param executor              a {@link OperationExecutor}
     * @param <C>                   the type of the configuration instance
     */
    <C> void registerOperationExecutor(Operation operation, C configurationInstance, OperationExecutor executor)
    {
        ConfigurationInstanceWrapper<C> wrapper = locateConfigurationInstanceWrapper(configurationInstance);
        wrapper.registerOperationExecutor(operation, executor);
    }

    Multimap<Configuration, ConfigurationInstanceWrapper<?>> getConfigurationInstances()
    {
        return ImmutableMultimap.copyOf(configurationInstances);
    }

    private <C> ConfigurationInstanceWrapper<C> locateConfigurationInstanceWrapper(final C configurationInstance)
    {
        ConfigurationInstanceWrapper<C> wrapper = (ConfigurationInstanceWrapper<C>) CollectionUtils.find(configurationInstances.values(), new Predicate()
        {
            @Override
            public boolean evaluate(Object object)
            {
                return ((ConfigurationInstanceWrapper<C>) object).getConfigurationInstance() == configurationInstance;
            }
        });

        checkState(wrapper != null, "Can't create an operation executor for an unregistered configuration instance");
        return wrapper;
    }
}
