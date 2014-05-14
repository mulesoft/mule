/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import org.mule.extensions.introspection.api.MuleExtensionOperation;
import org.mule.extensions.introspection.api.MuleExtensionOperationBuilder;
import org.mule.extensions.introspection.api.MuleExtensionParameterBuilder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

final class DefaultMuleExtensionOperationBuilder implements MuleExtensionOperationBuilder
{

    private String name;
    private String description;
    private List<Class<?>> inputTypes = new LinkedList<Class<?>>();
    private List<Class<?>> outputTypes = new LinkedList<Class<?>>();
    private List<MuleExtensionParameterBuilder> parameters = new LinkedList<MuleExtensionParameterBuilder>();
    private Set<String> ownerConfigurations = new HashSet<String>();


    DefaultMuleExtensionOperationBuilder()
    {
    }

    @Override
    public MuleExtensionOperationBuilder setName(String name)
    {
        this.name = name;
        return this;
    }

    @Override
    public MuleExtensionOperationBuilder setDescription(String description)
    {
        this.description = description;
        return this;
    }

    @Override
    public MuleExtensionOperationBuilder setInputTypes(Class<?>... inputTypes)
    {
        addAll(this.inputTypes, inputTypes);
        return this;
    }

    @Override
    public MuleExtensionOperationBuilder setOutputTypes(Class<?>... outputTypes)
    {
        addAll(this.outputTypes, outputTypes);
        return this;
    }

    @Override
    public MuleExtensionOperationBuilder addOwnerConfiguration(String name)
    {
        ownerConfigurations.add(name);
        return this;
    }

    @Override
    public MuleExtensionOperationBuilder addParameter(MuleExtensionParameterBuilder parameter)
    {
        parameters.add(parameter);
        return this;
    }

    @Override
    public MuleExtensionOperation build()
    {
        return new ImmutableMuleExtensionOperation(name,
                                                   description,
                                                   ownerConfigurations,
                                                   inputTypes,
                                                   outputTypes,
                                                   MuleExtensionUtils.build(parameters));
    }

    private <T> void addAll(List<T> list, T[] elements)
    {
        if (elements != null)
        {
            list.addAll(Arrays.asList(elements));
        }
    }
}
