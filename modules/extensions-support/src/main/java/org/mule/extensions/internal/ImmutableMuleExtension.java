/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import static org.mule.extensions.internal.MuleExtensionUtils.checkNullOrRepeatedNames;
import static org.mule.extensions.internal.MuleExtensionUtils.toMap;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.extensions.api.exception.NoSuchConfigurationException;
import org.mule.extensions.api.exception.NoSuchOperationException;
import org.mule.extensions.introspection.api.Capability;
import org.mule.extensions.introspection.api.MuleExtension;
import org.mule.extensions.introspection.api.MuleExtensionConfiguration;
import org.mule.extensions.introspection.api.MuleExtensionOperation;
import org.mule.extensions.introspection.api.MuleExtensionType;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

final class ImmutableMuleExtension extends AbstractImmutableDescribed implements MuleExtension
{

    private final String version;
    private final MuleExtensionType extensionType;
    private final String minMuleVersion;
    private final Map<String, MuleExtensionConfiguration> configurations;
    private final Map<String, MuleExtensionOperation> operations;
    private Map<Class<? extends Capability>, Capability> capabilities;

    protected ImmutableMuleExtension(String name,
                                     String description,
                                     String version,
                                     MuleExtensionType extensionType,
                                     String minMuleVersion,
                                     List<MuleExtensionConfiguration> configurations,
                                     List<MuleExtensionOperation> operations,
                                     Map<Class<? extends Capability>, Capability> capabilities)
    {
        super(name, description);

        checkNullOrRepeatedNames(configurations, "configurations");
        checkNullOrRepeatedNames(operations, "operations");

        checkArgument(!StringUtils.isBlank(version), "version cannot be blank");
        this.version = version;

        checkArgument(!StringUtils.isBlank(minMuleVersion), "minMuleVersion cannot be blank");
        this.minMuleVersion = minMuleVersion;

        checkArgument(extensionType != null, "extensionType cannot be null");
        this.extensionType = extensionType;

        this.configurations = toMap(configurations);
        this.operations = toMap(operations);
        this.capabilities = ImmutableMap.copyOf(capabilities);
    }



    @Override
    public List<MuleExtensionConfiguration> getConfigurations()
    {
        return ImmutableList.copyOf(configurations.values());
    }

    @Override
    public MuleExtensionConfiguration getConfiguration(String name) throws NoSuchConfigurationException
    {
        MuleExtensionConfiguration muleExtensionConfiguration = configurations.get(name);
        if (muleExtensionConfiguration == null)
        {
            throw new NoSuchConfigurationException(this, name);
        }

        return muleExtensionConfiguration;
    }

    @Override
    public List<MuleExtensionOperation> getOperations()
    {
        return ImmutableList.copyOf(operations.values());
    }

    @Override
    public String getVersion()
    {
        return version;
    }

    @Override
    public String getMinMuleVersion()
    {
        return minMuleVersion;
    }

    @Override
    public MuleExtensionType getExtensionType()
    {
        return extensionType;
    }

    @Override
    public MuleExtensionOperation getOperation(String name) throws NoSuchOperationException
    {
        MuleExtensionOperation muleExtensionOperation = operations.get(name);
        if (muleExtensionOperation == null)
        {
            throw new NoSuchOperationException(this, name);
        }

        return muleExtensionOperation;
    }

    @Override
    public <T extends Capability> Optional<T> getCapability(Class<T> capabilityType)
    {
        return (Optional<T>) Optional.fromNullable(capabilities.get(capabilityType));
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof MuleExtension)
        {
            MuleExtension other = (MuleExtension) obj;
            return Objects.equal(getName(), other.getName()) && Objects.equal(getVersion(), other.getVersion());
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(getName(), getVersion());
    }
}
