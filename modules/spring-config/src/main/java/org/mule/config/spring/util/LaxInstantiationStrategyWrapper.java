/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.util;

import org.mule.config.spring.OptionalObjectsController;
import org.mule.config.spring.processors.DiscardedOptionalBeanPostProcessor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.InstantiationStrategy;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * A {@link InstantiationStrategy} which doesn't fail if a bean
 * cannot be instantiated, as long as such bean has been marked as optional
 * in a {@link OptionalObjectsController}. In such case, the object
 * is marked as discarded with the controller and a placeholder object is returned.
 * <p/>
 * This object delegates actual instantiation into a {@code delegate} which
 * it wraps
 *
 * @since 3.7.0
 * @see DiscardedOptionalBeanPostProcessor
 */
public class LaxInstantiationStrategyWrapper implements InstantiationStrategy
{

    private final InstantiationStrategy delegate;
    private final OptionalObjectsController optionalObjectsController;

    public LaxInstantiationStrategyWrapper(InstantiationStrategy delegate, OptionalObjectsController optionalObjectsController)
    {
        this.delegate = delegate;
        this.optionalObjectsController = optionalObjectsController;
    }

    @Override
    public Object instantiate(RootBeanDefinition bd, String beanName, BeanFactory owner) throws BeansException
    {
        try
        {
            return delegate.instantiate(bd, beanName, owner);
        }
        catch (BeansException e)
        {
            return failIfNotOptional(e, beanName);
        }
    }

    @Override
    public Object instantiate(RootBeanDefinition bd, String beanName, BeanFactory owner, Constructor<?> ctor, Object... args) throws BeansException
    {
        try
        {
            return delegate.instantiate(bd, beanName, owner, ctor, args);
        }
        catch (BeansException e)
        {
            return failIfNotOptional(e, beanName);
        }
    }

    @Override
    public Object instantiate(RootBeanDefinition bd, String beanName, BeanFactory owner, Object factoryBean, Method factoryMethod, Object... args) throws BeansException
    {
        try
        {
            return delegate.instantiate(bd, beanName, owner, factoryBean, factoryMethod, args);
        }
        catch (BeansException e)
        {
            return failIfNotOptional(e, beanName);
        }
    }

    private Object failIfNotOptional(BeansException exception, String beanName) throws BeansException
    {
        if (optionalObjectsController.isOptional(beanName))
        {
            optionalObjectsController.discardOptionalObject(beanName);
            return optionalObjectsController.getDiscardedObjectPlaceholder();
        }
        else
        {
            throw exception;
        }
    }
}
