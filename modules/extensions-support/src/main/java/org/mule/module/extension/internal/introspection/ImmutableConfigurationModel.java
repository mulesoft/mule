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
import org.mule.extension.introspection.ConfigurationInstantiator;
import org.mule.extension.introspection.ConfigurationModel;
import org.mule.extension.introspection.ParameterModel;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Immutable implementation of {@link ConfigurationModel}
 *
 * @since 3.7.0
 */
final class ImmutableConfigurationModel extends AbstractImmutableModel implements ConfigurationModel
{

    private final List<ParameterModel> parameterModels;
    private final ConfigurationInstantiator instantiator;

    /**
     * Creates a new instance with the given state
     *
     * @param name            the configuration's name
     * @param description     the configuration's description
     * @param instantiator    the {@link ConfigurationInstantiator}. Cannot be {@code null}
     * @param parameterModels a {@link List} with the configuration's {@link ParameterModel parameterModels}
     * @param modelProperties A {@link Map} of custom properties which extend this model
     * @param capabilities    a {@link Set} with the configuration's capabilities
     * @throws IllegalArgumentException if {@code name} is blank
     */
    protected ImmutableConfigurationModel(String name,
                                          String description,
                                          ConfigurationInstantiator instantiator,
                                          List<ParameterModel> parameterModels,
                                          Map<String, Object> modelProperties,
                                          Set<Object> capabilities)
    {
        super(name, description, modelProperties, capabilities);
        validateRepeatedNames(parameterModels);
        checkArgument(instantiator != null, "instantiator cannot be null");

        this.parameterModels = immutableList(parameterModels);
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
}
