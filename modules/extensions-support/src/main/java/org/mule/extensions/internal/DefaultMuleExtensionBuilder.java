/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import org.mule.extensions.introspection.api.MuleExtension;
import org.mule.extensions.introspection.api.MuleExtensionBuilder;
import org.mule.extensions.introspection.api.MuleExtensionConfigurationBuilder;
import org.mule.extensions.introspection.api.MuleExtensionOperationBuilder;
import org.mule.extensions.introspection.api.MuleExtensionParameterBuilder;

import java.util.LinkedList;
import java.util.List;

public final class DefaultMuleExtensionBuilder implements MuleExtensionBuilder
{

    private String name;
    private String description;
    private String version;
    private List<MuleExtensionConfigurationBuilder> configurations = new LinkedList<MuleExtensionConfigurationBuilder>();
    private List<MuleExtensionOperationBuilder> operations = new LinkedList<MuleExtensionOperationBuilder>();

    public static MuleExtensionBuilder newBuilder()
    {
        return new DefaultMuleExtensionBuilder();
    }

    private DefaultMuleExtensionBuilder()
    {
    }

    @Override
    public MuleExtensionBuilder setName(String name)
    {
        this.name = name;
        return this;
    }

    @Override
    public MuleExtensionBuilder setDescription(String description)
    {
        this.description = description;
        return this;
    }

    @Override
    public MuleExtensionBuilder setVersion(String version)
    {
        this.version = version;
        return this;
    }

    @Override
    public MuleExtensionBuilder addConfiguration(MuleExtensionConfigurationBuilder configuration)
    {
        configurations.add(configuration);
        return this;
    }

    @Override
    public MuleExtensionBuilder addOperation(MuleExtensionOperationBuilder operation)
    {
        operations.add(operation);
        return this;
    }

    @Override
    public MuleExtension build()
    {
        return new ImmutableMuleExtension(name,
                                          description,
                                          version,
                                          MuleExtensionUtils.build(configurations),
                                          MuleExtensionUtils.build(operations));
    }

    @Override
    public MuleExtensionConfigurationBuilder newConfiguration()
    {
        return new DefaultMuleExtensionConfigurationBuilder();
    }

    @Override
    public MuleExtensionOperationBuilder newOperation()
    {
        return new DefaultMuleExtensionOperationBuilder();
    }

    @Override
    public MuleExtensionParameterBuilder newParameter()
    {
        return new DefaultMuleExtensionParameterBuilder();
    }
}
