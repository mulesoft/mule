/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import static org.mule.util.Preconditions.checkArgument;

import org.springframework.beans.factory.FactoryBean;

public class FixedFactoryBean<T> implements FactoryBean<T>
{

    private final T value;

    public FixedFactoryBean(T value)
    {
        checkArgument(value != null, "value cannot be null");
        this.value = value;
    }

    @Override
    public T getObject() throws Exception
    {
        return value;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }

    @Override
    public Class<?> getObjectType()
    {
        return value.getClass();
    }
}
