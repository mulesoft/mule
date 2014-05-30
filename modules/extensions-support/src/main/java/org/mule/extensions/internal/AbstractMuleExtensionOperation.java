/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import static org.mule.extensions.internal.MuleExtensionUtils.immutableList;
import org.mule.extensions.introspection.api.MuleExtensionConfiguration;
import org.mule.extensions.introspection.api.MuleExtensionOperation;
import org.mule.extensions.introspection.api.MuleExtensionParameter;
import org.mule.extensions.introspection.api.OperationContext;
import org.mule.util.Preconditions;

import java.util.List;
import java.util.Set;

abstract class AbstractMuleExtensionOperation extends AbstractImmutableDescribed implements MuleExtensionOperation
{

    protected final Set<String> ownerConfigurations;
    protected final List<Class<?>> inputTypes;
    protected final List<Class<?>> outputTypes;
    protected final List<MuleExtensionParameter> parameters;

    AbstractMuleExtensionOperation(String name,
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
    public List<Class<?>> getOutputTypes()
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
