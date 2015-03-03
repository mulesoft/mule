/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import static org.mule.module.extension.internal.util.MuleExtensionUtils.checkNullOrRepeatedNames;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.ConfigurationInstantiator;
import org.mule.extension.introspection.Parameter;
import org.mule.module.extension.internal.util.MuleExtensionUtils;

import java.util.List;
import java.util.Set;

/**
 * Immutable implementation of {@link Configuration}
 *
 * @since 3.7.0
 */
final class ImmutableConfiguration extends AbstractImmutableCapableDescribed implements Configuration
{

    private final List<Parameter> parameters;
    private final ConfigurationInstantiator instantiator;

    protected ImmutableConfiguration(String name,
                                     String description,
                                     ConfigurationInstantiator instantiator,
                                     List<Parameter> parameters,
                                     Set<Object> capabilities)
    {
        super(name, description, capabilities);
        checkNullOrRepeatedNames(parameters, "parameters");

        this.parameters = MuleExtensionUtils.immutableList(parameters);
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
