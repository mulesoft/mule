/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import org.mule.extension.introspection.ConfigurationModel;

/**
 * Groups configuration's instance related information
 *
 * @since 4.0
 */
public final class DeclaredConfiguration<T>
{

    private final String name;
    private final ConfigurationModel model;
    private final T value;

    public DeclaredConfiguration(String name, ConfigurationModel model, T value)
    {
        this.name = name;
        this.model = model;
        this.value = value;
    }

    public Object getValue()
    {
        return value;
    }

    public String getName()
    {
        return name;
    }

    public ConfigurationModel getModel()
    {
        return model;
    }
}
