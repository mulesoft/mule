/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import org.mule.extensions.introspection.api.MuleExtensionParameter;
import org.mule.extensions.introspection.api.MuleExtensionParameterBuilder;

final class DefaultMuleExtensionParameterBuilder implements MuleExtensionParameterBuilder
{

    private String name;
    private String description;
    private Class<?> type;
    private boolean required = false;
    private boolean acceptsExpressions = true;
    private Object defaultValue;

    DefaultMuleExtensionParameterBuilder()
    {
    }

    @Override
    public MuleExtensionParameterBuilder setName(String name)
    {
        this.name = name;
        return this;
    }

    @Override
    public MuleExtensionParameterBuilder setDescription(String description)
    {
        this.description = description;
        return this;
    }

    @Override
    public MuleExtensionParameterBuilder setType(Class<?> type)
    {
        this.type = type;
        return this;
    }

    @Override
    public MuleExtensionParameterBuilder setRequired(boolean required)
    {
        this.required = required;
        return this;
    }

    @Override
    public MuleExtensionParameterBuilder setAcceptsExpressions(boolean acceptsExpressions)
    {
        this.acceptsExpressions = acceptsExpressions;
        return this;
    }

    @Override
    public MuleExtensionParameterBuilder setDefaultValue(Object defaultValue)
    {
        this.defaultValue = defaultValue;
        return this;
    }

    @Override
    public MuleExtensionParameter build()
    {
        if (required && defaultValue != null)
        {
            throw new IllegalStateException("If a parameter is required then it cannot have a default value");
        }

        return new ImmutableMuleExtensionParameter(name, description, type, required, acceptsExpressions, defaultValue);
    }
}
