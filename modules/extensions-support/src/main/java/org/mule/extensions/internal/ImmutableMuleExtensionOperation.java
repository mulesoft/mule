/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import static org.mule.extensions.internal.MuleExtensionUtils.checkNullOrRepeatedNames;
import static org.mule.extensions.internal.MuleExtensionUtils.immutableList;
import org.mule.extensions.introspection.api.MuleExtensionConfiguration;
import org.mule.extensions.introspection.api.MuleExtensionOperation;
import org.mule.extensions.introspection.api.MuleExtensionParameter;
import org.mule.extensions.introspection.api.OperationContext;
import org.mule.util.Preconditions;

import java.util.List;
import java.util.Set;

final class ImmutableMuleExtensionOperation extends AbstractImmutableDescribed implements MuleExtensionOperation
{

    private final Set<String> ownerConfigurations;
    private final List<Class<?>> inputTypes;
    private final List<Class<?>> outputTypes;
    private final List<MuleExtensionParameter> parameters;


    protected ImmutableMuleExtensionOperation(String name,
                                              String description,
                                              Set<String> ownerConfigurations,
                                              List<Class<?>> inputTypes,
                                              List<Class<?>> outputTypes,
                                              List<MuleExtensionParameter> parameters)
    {
        super(name, description);
        checkNullOrRepeatedNames(parameters, "parameters");

        this.ownerConfigurations = ownerConfigurations;
        this.inputTypes = immutableList(inputTypes);
        this.outputTypes = immutableList(outputTypes);
        this.parameters = immutableList(parameters);
    }

    @Override
    public List<MuleExtensionParameter> getParameters()
    {
        return parameters;
    }

    @Override
    public List<Class<?>> getInputTypes()
    {
        return inputTypes;
    }

    @Override
    public List<Class<?>> getOutputType()
    {
        return outputTypes;
    }

    @Override
    public OperationContext prepare()
    {
        return null;
    }

    @Override
    public boolean isAvailableFor(MuleExtensionConfiguration muleExtensionConfiguration)
    {
        Preconditions.checkArgument(muleExtensionConfiguration != null, "configuration cannot be null");

        return ownerConfigurations.isEmpty()
               ? true
               : ownerConfigurations.contains(muleExtensionConfiguration.getName());
    }
}
