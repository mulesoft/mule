/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.introspection;

import static org.mule.module.extensions.internal.util.MuleExtensionUtils.checkNamesClashes;
import static org.mule.module.extensions.internal.util.MuleExtensionUtils.checkNullOrRepeatedNames;
import static org.mule.module.extensions.internal.util.MuleExtensionUtils.toMap;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.extensions.introspection.Configuration;
import org.mule.extensions.introspection.Extension;
import org.mule.extensions.introspection.NoSuchConfigurationException;
import org.mule.extensions.introspection.NoSuchOperationException;
import org.mule.extensions.introspection.Operation;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * Immutable implementation of {@link Extension}
 *
 * @since 3.7.0
 */
final class ImmutableExtension extends AbstractImmutableCapableDescribed implements Extension
{

    private final String version;
    private final Map<String, Configuration> configurations;
    private final Map<String, Operation> operations;

    protected ImmutableExtension(String name,
                                 String description,
                                 String version,
                                 List<Configuration> configurations,
                                 List<Operation> operations,
                                 Set<Object> capabilities)
    {
        super(name, description, capabilities);

        checkArgument(!name.contains(" "), "Extension name cannot contain spaces");
        checkNullOrRepeatedNames(configurations, "configurations");
        checkNullOrRepeatedNames(operations, "operations");
        checkNamesClashes(configurations, operations);

        this.configurations = toMap(configurations);
        this.operations = toMap(operations);

        checkArgument(!StringUtils.isBlank(version), "version cannot be blank");
        this.version = version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Configuration> getConfigurations()
    {
        return ImmutableList.copyOf(configurations.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration getConfiguration(String name) throws NoSuchConfigurationException
    {
        Configuration configuration = configurations.get(name);
        if (configuration == null)
        {
            throw new NoSuchConfigurationException(this, name);
        }

        return configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Operation> getOperations()
    {
        return ImmutableList.copyOf(operations.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion()
    {
        return version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Operation getOperation(String name) throws NoSuchOperationException
    {
        Operation operation = operations.get(name);
        if (operation == null)
        {
            throw new NoSuchOperationException(this, name);
        }

        return operation;
    }
}
