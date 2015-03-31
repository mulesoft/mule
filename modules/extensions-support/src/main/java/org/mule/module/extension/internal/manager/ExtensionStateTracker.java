/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Operation;
import org.mule.extension.runtime.OperationExecutor;

import com.google.common.collect.Multimap;

/**
 * Holds state regarding the use that the platform is doing of a
 * certain {@link Extension}
 *
 * @since 3.7.0
 */
final class ExtensionStateTracker
{

    private final ConfigurationsStateTracker configurationsState = new ConfigurationsStateTracker();

    /**
     * Registers a {@code configurationInstance} which is a realization of a {@link Configuration}
     * model defined by {@code configuration}
     *
     * @param instanceName                  the name of the instance
     * @param configuration         a {@link Configuration}
     * @param configurationInstance an instance which is compliant with the {@code configuration} model
     * @param <C>                   the type of the configuration instance
     */
    <C> void registerConfigurationInstance(String instanceName, Configuration configuration, C configurationInstance)
    {
        configurationsState.registerInstance(configuration, instanceName, configurationInstance);
    }

    Multimap<Configuration, ConfigurationInstanceWrapper<?>> getConfigurationInstances()
    {
        return configurationsState.getConfigurationInstances();
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
        return configurationsState.getOperationExecutor(operation, configurationInstance);
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
        configurationsState.registerOperationExecutor(operation, configurationInstance, executor);
    }
}
