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
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.ConfigurationInstantiator;
import org.mule.extension.introspection.Parameter;

import java.util.List;
import java.util.Set;

/**
 * Immutable implementation of {@link Configuration}
 *
 * @since 3.7.0
 */
final class ImmutableConfiguration extends AbstractCapableDescribed implements Configuration
{

    private final List<Parameter> parameters;
    private final ConfigurationInstantiator instantiator;

    /**
     * Creates a new instance with the given state
     *
     * @param name         the configuration's name
     * @param description  the configuration's description
     * @param instantiator the {@link ConfigurationInstantiator}. Cannot be {@code null}
     * @param parameters   a {@link List} with the configuration's {@link Parameter}s
     * @param capabilities a {@link Set} with the configuration's capabilities
     */
    protected ImmutableConfiguration(String name,
                                     String description,
                                     ConfigurationInstantiator instantiator,
                                     List<Parameter> parameters,
                                     Set<Object> capabilities)
    {
        super(name, description, capabilities);
        validateRepeatedNames(parameters);
        checkArgument(instantiator != null, "instantiator cannot be null");

        this.parameters = immutableList(parameters);
        this.instantiator = instantiator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final List<Parameter> getParameters()
    {
        return parameters;
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
