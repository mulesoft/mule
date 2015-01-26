/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.util;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

public final class InitialisingBeanDefintionRegistry implements BeanDefinitionRegistry
{

    private final BeanDefinitionRegistry delegate;

    public InitialisingBeanDefintionRegistry(BeanDefinitionRegistry delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionStoreException
    {
        delegate.registerBeanDefinition(beanName, beanDefinition);
    }

    @Override
    public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException
    {
        delegate.removeBeanDefinition(beanName);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException
    {
        return delegate.getBeanDefinition(beanName);
    }

    @Override
    public boolean containsBeanDefinition(String beanName)
    {
        return delegate.containsBeanDefinition(beanName);
    }

    @Override
    public String[] getBeanDefinitionNames()
    {
        return delegate.getBeanDefinitionNames();
    }

    @Override
    public int getBeanDefinitionCount()
    {
        return delegate.getBeanDefinitionCount();
    }

    @Override
    public boolean isBeanNameInUse(String beanName)
    {
        return delegate.isBeanNameInUse(beanName);
    }

    @Override
    public void registerAlias(String name, String alias)
    {
        delegate.registerAlias(name, alias);
    }

    @Override
    public void removeAlias(String alias)
    {
        delegate.removeAlias(alias);
    }

    @Override
    public boolean isAlias(String beanName)
    {
        return delegate.isAlias(beanName);
    }

    @Override
    public String[] getAliases(String name)
    {
        return delegate.getAliases(name);
    }
}
