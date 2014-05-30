/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal.spi;

class ParameterDescriptor
{

    private String name;
    private Class<?> type;
    private boolean required;
    private Object defaultValue;

    String getName()
    {
        return name;
    }

    void setName(String name)
    {
        this.name = name;
    }

    Class<?> getType()
    {
        return type;
    }

    void setType(Class<?> type)
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
}
