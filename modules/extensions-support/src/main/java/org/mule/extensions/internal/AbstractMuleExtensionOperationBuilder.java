/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import org.mule.extensions.introspection.api.ExtensionOperation;
import org.mule.extensions.introspection.spi.MuleExtensionParameterBuilder;
import org.mule.extensions.introspection.spi.OperationBuilder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

abstract class AbstractMuleExtensionOperationBuilder<T extends ExtensionOperation, B> implements OperationBuilder<T, B>
{

    protected String name;
    protected String description;
    protected List<Class<?>> inputTypes = new LinkedList<Class<?>>();
    protected List<Class<?>> outputTypes = new LinkedList<Class<?>>();
    protected List<MuleExtensionParameterBuilder> parameters = new LinkedList<MuleExtensionParameterBuilder>();
    protected Set<String> ownerConfigurations = new HashSet<String>();

    protected abstract B chain();

    @Override
    public B setName(String name)
    {
        this.name = name;
        return chain();
    }

    @Override
    public B setDescription(String description)
    {
        this.description = description;
        return chain();
    }

    @Override
    public B setInputTypes(Class<?>... inputTypes)
    {
        addAll(this.inputTypes, inputTypes);
        return chain();
    }

    @Override
    public B setOutputTypes(Class<?>... outputTypes)
    {
        addAll(this.outputTypes, outputTypes);
        return chain();
    }

    @Override
    public B addOwnerConfiguration(String name)
    {
        ownerConfigurations.add(name);
        return chain();
    }

    @Override
    public B addParameter(MuleExtensionParameterBuilder parameter)
    {
        parameters.add(parameter);
        return chain();
    }

    protected <T> void addAll(List<T> list, T[] elements)
    {
        if (elements != null)
        {
            list.addAll(Arrays.asList(elements));
        }
    }
}
