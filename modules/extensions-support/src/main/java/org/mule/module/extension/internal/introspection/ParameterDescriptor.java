/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection;

import org.mule.extension.introspection.DataType;

/**
 * Intermediate representation of a parameter used to decouple
 * an interchangeable introspection mechanism from the extension's API
 * model
 *
 * @since 3.7.0
 */
final class ParameterDescriptor
{

    private String name;
    private DataType type;
    private boolean required;
    private Object defaultValue;
    private boolean hidden = false;
    private Class<?> typeRestriction = null;

    String getName()
    {
        return name;
    }

    void setName(String name)
    {
        this.name = name;
    }

    DataType getType()
    {
        return type;
    }

    void setType(DataType type)
    {
        this.type = type;
    }

    boolean isRequired()
    {
        return required;
    }

    void setRequired(boolean required)
    {
        this.required = required;
    }

    Object getDefaultValue()
    {
        return defaultValue;
    }

    void setDefaultValue(Object defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    boolean isHidden()
    {
        return hidden;
    }

    void setHidden(boolean hidden)
    {
        this.hidden = hidden;
    }

    Class<?> getTypeRestriction()
    {
        return typeRestriction;
    }

    void setTypeRestriction(Class<?> typeRestriction)
    {
        this.typeRestriction = typeRestriction;
    }
}
