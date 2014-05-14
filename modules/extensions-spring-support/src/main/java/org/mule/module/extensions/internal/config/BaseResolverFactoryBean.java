/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.config;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.module.extensions.internal.runtime.resolver.ConfigurationValueResolver;
import org.mule.module.extensions.internal.runtime.resolver.ValueResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;

abstract class BaseResolverFactoryBean<T extends ValueResolver> implements FactoryBean<T> , MuleContextAware, Initialisable, Disposable
{
    protected final Logger logger = LoggerFactory.getLogger(getObjectType());

    protected final String name;
    protected ElementDescriptor element;
    protected T valueResolver;
    protected MuleContext muleContext;

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

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    /**
     * Propagates the {@link #muleContext} and initialisation event
     * to the underlying {@link #valueResolver} in case it implements the
     * {@link MuleContextAware} and/or {@link Initialisable} interfaces
     *
     * @throws InitialisationException
     */
    @Override
    public void initialise() throws InitialisationException
    {
        LifecycleUtils.initialiseIfNeeded(valueResolver, muleContext);
    }

    /**
     * Propagates the disposal event to the
     * underlying {@link #valueResolver} if it implements
     * the {@link Disposable} interface
     */
    @Override
    public void dispose()
    {
        LifecycleUtils.disposeIfNeeded(valueResolver, logger);
    }

}
