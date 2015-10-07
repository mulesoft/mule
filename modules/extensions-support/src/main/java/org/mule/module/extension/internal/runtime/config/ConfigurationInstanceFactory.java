/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.config;

import static org.mule.module.extension.internal.util.MuleExtensionUtils.createInterceptors;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.runtime.ConfigurationInstance;
import org.mule.extension.api.runtime.InterceptorFactory;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;

/**
 * Reusable and thread-safe factory that creates instances of {@link ConfigurationInstance}
 * <p>
 * The created instances will be of concrete type {@link LifecycleAwareConfigurationInstance}, which means
 * that all the {@link InterceptorFactory interceptor factories} obtained through
 * {@link ConfigurationModel#getInterceptorFactories()}  will be exercised per each
 * created instance
 *
 * @param <T> the generic type of the returned {@link ConfigurationInstance} instances
 * @since 4.0
 */
public final class ConfigurationInstanceFactory<T>
{

    private final ConfigurationModel configurationModel;
    private final ConfigurationObjectBuilder<T> configurationObjectBuilder;

    /**
     * Creates a new instance which provides instances derived from the given {@code configurationModel}
     * and {@code resolverSet}
     *
     * @param configurationModel the {@link ConfigurationModel} that describes the configurations to be created
     * @param resolverSet        the {@link ResolverSet} which provides the values for the configuration's parameters
     */
    public ConfigurationInstanceFactory(ConfigurationModel configurationModel, ResolverSet resolverSet)
    {
        this.configurationModel = configurationModel;
        configurationObjectBuilder = new ConfigurationObjectBuilder<>(configurationModel, resolverSet);
    }

    /**
     * Creates a new instance using the given {@code event} to obtain the configuration's parameter
     * values
     *
     * @param name  the name of the configuration to return
     * @param event the current {@link MuleEvent}
     * @return a {@link ConfigurationInstance}
     * @throws MuleException if an error is encountered
     */
    public ConfigurationInstance<T> createConfiguration(String name, MuleEvent event) throws MuleException
    {
        return new LifecycleAwareConfigurationInstance<>(name,
                                                         configurationModel,
                                                         createConfigurationInstance(event),
                                                         createInterceptors(configurationModel));

    }

    /**
     * Creates a new instance using the given {@code resolverSetResult} to obtain the configuration's parameter
     * values
     *
     * @param name              the name of the configuration to return
     * @param resolverSetResult the {@link ResolverSetResult} with previously evaluated values
     * @return a {@link ConfigurationInstance}
     * @throws MuleException if an error is encountered
     */
    public ConfigurationInstance<T> createConfiguration(String name, ResolverSetResult resolverSetResult) throws MuleException
    {
        return new LifecycleAwareConfigurationInstance<>(name,
                                                         configurationModel,
                                                         createConfigurationInstance(resolverSetResult),
                                                         createInterceptors(configurationModel));

    }

    private T createConfigurationInstance(ResolverSetResult resolverSetResult) throws MuleException
    {
        return configurationObjectBuilder.build(resolverSetResult);
    }

    private T createConfigurationInstance(MuleEvent event) throws MuleException
    {
        return configurationObjectBuilder.build(event);
    }
}
