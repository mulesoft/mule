/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import static org.mule.extensions.internal.MuleExtensionUtils.immutableList;
import org.mule.extensions.introspection.api.ExtensionConfiguration;
import org.mule.extensions.introspection.api.ExtensionOperation;
import org.mule.extensions.introspection.api.MuleExtensionParameter;
import org.mule.util.Preconditions;

import java.util.List;
import java.util.Set;

/**
 * Convenience superclass for implementations of {@link org.mule.extensions.introspection.api.ExtensionOperation}
 *
 * @since 1.0
 */
abstract class AbstractExtensionOperation extends AbstractImmutableDescribed implements ExtensionOperation
{

    protected final Set<String> ownerConfigurations;
    protected final List<Class<?>> inputTypes;
    protected final List<Class<?>> outputTypes;
    protected final List<MuleExtensionParameter> parameters;

    AbstractExtensionOperation(String name,
                               String description,
                               Set<String> ownerConfigurations,
                               List<Class<?>> inputTypes,
                               List<Class<?>> outputTypes,
                               List<MuleExtensionParameter> parameters)
    {
        super(name, description);

        this.inputTypes = immutableList(inputTypes);
        this.outputTypes = immutableList(outputTypes);
        this.parameters = immutableList(parameters);
        this.ownerConfigurations = ownerConfigurations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<MuleExtensionParameter> getParameters()
    {
        return parameters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Class<?>> getInputTypes()
    {
        return inputTypes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Class<?>> getOutputTypes()
    {
        return outputTypes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAvailableFor(ExtensionConfiguration extensionConfiguration)
    {
        Preconditions.checkArgument(extensionConfiguration != null, "configuration cannot be null");

        return ownerConfigurations.isEmpty()
               ? true
               : ownerConfigurations.contains(extensionConfiguration.getName());
    }
}
