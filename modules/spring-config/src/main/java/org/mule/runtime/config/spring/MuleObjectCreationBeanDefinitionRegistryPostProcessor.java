/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import java.util.function.Consumer;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * Simple {@link org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor} to
 * delegate to a consumer function when invoked.
 *
 * @since 4.0
 */
public class MuleObjectCreationBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor
{

    private final Consumer<BeanDefinitionRegistry> beanDefinitionRegistryProcessor;

    public MuleObjectCreationBeanDefinitionRegistryPostProcessor(Consumer<BeanDefinitionRegistry> beanDefinitionRegistryProcessor)
    {
        this.beanDefinitionRegistryProcessor = beanDefinitionRegistryProcessor;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException
    {
        //Nothing to do.
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException
    {
        beanDefinitionRegistryProcessor.accept(registry);
    }

}



