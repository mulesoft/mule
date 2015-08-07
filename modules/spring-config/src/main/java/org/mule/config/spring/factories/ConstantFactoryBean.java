/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} which returns a fixed instanced obtained
 * through the constructor. {@link #isSingleton()} always returns {@code true}.
 * <p/>
 * Invocations related to the {@link MuleContextAware} and {@link Lifecycle} interfaces
 * are delegated into the {@link #value} object when applies.
 *
 * @param <T>
 * @since 3.7.0
 */
public class ConstantFactoryBean<T> implements FactoryBean<T>, MuleContextAware, Lifecycle
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ConstantFactoryBean.class);

    private final T value;

    public ConstantFactoryBean(T value)
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

    @Override
    public void setMuleContext(MuleContext context)
    {
        if (value instanceof MuleContextAware)
        {
            ((MuleContextAware) value).setMuleContext(context);
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        LifecycleUtils.initialiseIfNeeded(value);
    }

    @Override
    public void start() throws MuleException
    {
        LifecycleUtils.startIfNeeded(value);
    }

    @Override
    public void stop() throws MuleException
    {
        LifecycleUtils.stopIfNeeded(value);
    }

    @Override
    public void dispose()
    {
        LifecycleUtils.disposeIfNeeded(value, LOGGER);
    }
}
