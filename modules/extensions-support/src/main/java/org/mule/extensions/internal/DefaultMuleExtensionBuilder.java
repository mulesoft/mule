/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.extensions.introspection.api.Capability;
import org.mule.extensions.introspection.api.MuleExtension;
import org.mule.extensions.introspection.api.MuleExtensionBuilder;
import org.mule.extensions.introspection.api.MuleExtensionConfigurationBuilder;
import org.mule.extensions.introspection.api.MuleExtensionOperationBuilder;
import org.mule.extensions.introspection.api.MuleExtensionParameterBuilder;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class DefaultMuleExtensionBuilder implements MuleExtensionBuilder
{

    private String name;
    private String description;
    private String version;
    private List<MuleExtensionConfigurationBuilder> configurations = new LinkedList<MuleExtensionConfigurationBuilder>();
    private List<MuleExtensionOperationBuilder> operations = new LinkedList<MuleExtensionOperationBuilder>();
    private Map<Class<? extends Capability>, Capability> capabilities = new HashMap<Class<? extends Capability>, Capability>();

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
        checkArgument(configuration != null, "cannot add a null configuration builder");
        configurations.add(configuration);

        return this;
    }

    @Override
    public MuleExtensionBuilder addOperation(MuleExtensionOperationBuilder operation)
    {
        checkArgument(operation != null, "Cannot add a null operation builder");
        operations.add(operation);

        return this;
    }

    @Override
    public <T extends Capability, C extends T> MuleExtensionBuilder addCapablity(Class<T> capabilityType, C capability)
    {
        checkArgument(capabilityType != null, "capabilityType cannot be null");
        checkArgument(capability != null, "capability cannot be null");

        capabilities.put(capabilityType, capability);
        return this;
    }

    @Override
    public MuleExtension build()
    {
        return new ImmutableMuleExtension(name,
                                          description,
                                          version,
                                          MuleExtensionUtils.build(configurations),
                                          MuleExtensionUtils.build(operations),
                                          capabilities);
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
