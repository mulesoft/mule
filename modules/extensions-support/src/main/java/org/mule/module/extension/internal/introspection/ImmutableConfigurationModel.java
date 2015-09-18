/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.mule.module.extension.internal.util.MuleExtensionUtils.validateRepeatedNames;
import static org.mule.util.CollectionUtils.immutableList;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.extension.introspection.ConfigurationInstantiator;
import org.mule.api.extension.introspection.ConfigurationModel;
import org.mule.api.extension.introspection.ExtensionModel;
import org.mule.api.extension.introspection.ParameterModel;
import org.mule.api.extension.runtime.InterceptorFactory;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Immutable implementation of {@link ConfigurationModel}
 *
 * @since 3.7.0
 */
final class ImmutableConfigurationModel extends AbstractImmutableModel implements ConfigurationModel
{

    private final Supplier<ExtensionModel> extensionModelSupplier;
    private final List<ParameterModel> parameterModels;
    private final List<InterceptorFactory> interceptorFactories;
    private final ConfigurationInstantiator instantiator;

    /**
     * Creates a new instance with the given state
     *
     * @param name                 the configuration's name
     * @param description          the configuration's description
     * @param instantiator         the {@link ConfigurationInstantiator}. Cannot be {@code null}
     * @param parameterModels      a {@link List} with the configuration's {@link ParameterModel parameterModels}
     * @param modelProperties      A {@link Map} of custom properties which extend this model
     * @param interceptorFactories A {@link List} with the {@link InterceptorFactory} instances that should be applied to instances built from this model
     * @throws IllegalArgumentException if {@code name} is blank
     */
    protected ImmutableConfigurationModel(String name,
                                          String description,
                                          Supplier<ExtensionModel> extensionModelSupplier,
                                          ConfigurationInstantiator instantiator,
                                          List<ParameterModel> parameterModels,
                                          Map<String, Object> modelProperties,
                                          List<InterceptorFactory> interceptorFactories)
    {
        super(name, description, modelProperties);
        validateRepeatedNames(parameterModels);
        checkArgument(instantiator != null, "instantiator cannot be null");

        this.extensionModelSupplier = extensionModelSupplier;
        this.parameterModels = immutableList(parameterModels);
        this.interceptorFactories = interceptorFactories != null ? ImmutableList.copyOf(interceptorFactories) : ImmutableList.of();
        this.instantiator = instantiator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final List<ParameterModel> getParameterModels()
    {
        return parameterModels;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationInstantiator getInstantiator()
    {
        return instantiator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<InterceptorFactory> getInterceptorFactories()
    {
        return interceptorFactories;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExtensionModel getExtensionModel()
    {
        return extensionModelSupplier.get();
    }
}
