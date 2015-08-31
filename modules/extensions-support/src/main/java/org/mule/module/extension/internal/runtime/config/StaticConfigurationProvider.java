/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.config;

import org.mule.extension.introspection.ConfigurationModel;
import org.mule.extension.introspection.OperationModel;
import org.mule.extension.runtime.ConfigurationProvider;
import org.mule.extension.runtime.OperationContext;

/**
 * {@link ConfigurationProvider} which provides always the same {@link #configuration}.
 *
 * @param <T> the generic type of the instances provided
 * @since 3.7.0
 */
public final class StaticConfigurationProvider<T> implements ConfigurationProvider<T>
{

    private final String name;
    private final ConfigurationModel model;
    private final T configuration;

    public StaticConfigurationProvider(String name, ConfigurationModel model, T configuration)
    {
        this.name = name;
        this.model = model;
        this.configuration = configuration;
    }

    /**
     * Returns {@link #configuration}.
     * <p/>
     * The first time this method is invoked, the instance
     * is registered on the {@code registrationCallback}
     *
     * @param operationContext the {@link OperationContext context} of the {@link OperationModel} being executed
     * @return {@link #configuration}
     */
    @Override
    public T get(OperationContext operationContext)
    {
        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationModel getModel()
    {
        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return name;
    }
}
