/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import org.mule.module.extension.internal.runtime.resolver.ConfigurationValueResolver;
import org.mule.module.extension.internal.runtime.resolver.ValueResolver;

import org.springframework.beans.factory.FactoryBean;

abstract class BaseResolverFactoryBean<T extends ValueResolver> implements FactoryBean<T>
{
    protected final String name;
    protected ElementDescriptor element;
    protected T valueResolver;

    BaseResolverFactoryBean(String name, ElementDescriptor element)
    {
        this.name = name;
        this.element = element;
    }

    protected abstract T createValueResolver();

    /**
     * Returns a {@link ConfigurationValueResolver}
     */
    @Override
    public T getObject() throws Exception
    {
        return valueResolver;
    }

    /**
     * @return {@value true}
     */
    @Override
    public boolean isSingleton()
    {
        return true;
    }
}
