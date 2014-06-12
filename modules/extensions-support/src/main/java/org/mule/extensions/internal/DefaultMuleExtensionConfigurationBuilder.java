/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import static org.mule.extensions.introspection.api.ExtensionConfiguration.DEFAULT_DESCRIPTION;
import static org.mule.extensions.introspection.api.ExtensionConfiguration.DEFAULT_NAME;
import org.mule.extensions.introspection.api.ExtensionConfiguration;
import org.mule.extensions.introspection.spi.MuleExtensionConfigurationBuilder;
import org.mule.extensions.introspection.spi.MuleExtensionParameterBuilder;

import java.util.LinkedList;
import java.util.List;

final class DefaultMuleExtensionConfigurationBuilder implements MuleExtensionConfigurationBuilder
{

    private String name = DEFAULT_NAME;
    private String description = DEFAULT_DESCRIPTION;
    private List<MuleExtensionParameterBuilder> parameters = new LinkedList<MuleExtensionParameterBuilder>();

    DefaultMuleExtensionConfigurationBuilder()
    {
    }

    @Override
    public MuleExtensionConfigurationBuilder setName(String name)
    {
        this.name = name;
        return this;
    }

    @Override
    public MuleExtensionConfigurationBuilder setDescription(String description)
    {
        this.description = description;
        return this;
    }

    @Override
    public MuleExtensionConfigurationBuilder addParameter(MuleExtensionParameterBuilder parameter)
    {
        parameters.add(parameter);
        return this;
    }

    @Override
    public ExtensionConfiguration build()
    {
        return new ImmutableExtensionConfiguration(name, description, MuleExtensionUtils.build(parameters));
    }

}
