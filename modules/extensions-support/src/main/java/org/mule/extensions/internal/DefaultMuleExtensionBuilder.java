/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import static org.mule.util.Preconditions.checkArgument;
import static org.mule.util.Preconditions.checkState;
import org.mule.common.MuleVersion;
import org.mule.extensions.introspection.api.Capability;
import org.mule.extensions.introspection.api.MuleExtension;
import org.mule.extensions.introspection.api.MuleExtensionOperation;
import org.mule.extensions.introspection.api.MuleExtensionType;
import org.mule.extensions.introspection.spi.Builder;
import org.mule.extensions.introspection.spi.MuleExtensionBuilder;
import org.mule.extensions.introspection.spi.MuleExtensionConfigurationBuilder;
import org.mule.extensions.introspection.spi.MuleExtensionOperationBuilder;
import org.mule.extensions.introspection.spi.MuleExtensionOperationGroupBuilder;
import org.mule.extensions.introspection.spi.MuleExtensionParameterBuilder;
import org.mule.extensions.introspection.spi.MuleExtensionScopeBuilder;
import org.mule.extensions.introspection.spi.OperationBuilder;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public final class DefaultMuleExtensionBuilder implements MuleExtensionBuilder
{

    private String name;
    private String description;
    private String version;
    private MuleExtensionType extensionType;
    private String minMuleVersion;
    private List<MuleExtensionConfigurationBuilder> configurations = new LinkedList<MuleExtensionConfigurationBuilder>();
    private List<Builder<MuleExtensionOperation>> operations = new LinkedList<Builder<MuleExtensionOperation>>();
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
    public MuleExtensionBuilder setExtensionType(MuleExtensionType extensionType)
    {
        this.extensionType = extensionType;
        return this;
    }

    @Override
    public MuleExtensionBuilder setMinMuleVersion(String minMuleVersion)
    {
        this.minMuleVersion = minMuleVersion;
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
    public <T extends MuleExtensionOperation, B> MuleExtensionBuilder addOperation(OperationBuilder<T, B> operation)
    {
        checkArgument(operation != null, "Cannot add a null operation builder");
        operations.add((Builder<MuleExtensionOperation>) operation);

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
        validateMuleVersion();
        return new ImmutableMuleExtension(name,
                                          description,
                                          version,
                                          extensionType,
                                          minMuleVersion,
                                          MuleExtensionUtils.build(configurations),
                                          MuleExtensionUtils.build(operations),
                                          capabilities);
    }

    private void validateMuleVersion()
    {
        checkState(!StringUtils.isBlank(minMuleVersion), "minimum Mule version cannot be blank");
        checkState(new MuleVersion(minMuleVersion).atLeast(DEFAULT_MIN_MULE_VERSION),
                   String.format("Minimum Mule version must be at least %s", DEFAULT_MIN_MULE_VERSION.toString()));
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
    public MuleExtensionScopeBuilder newScope()
    {
        return new DefaultMuleExtensionScopeBuilder();
    }

    @Override
    public MuleExtensionOperationGroupBuilder newOperationGroup()
    {
        return new DefaultMuleExtensionOperationGroupBuilder();
    }

    @Override
    public MuleExtensionParameterBuilder newParameter()
    {
        return new DefaultMuleExtensionParameterBuilder();
    }
}
