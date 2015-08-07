/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.processors;

import org.mule.registry.MuleRegistryHelper;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * A {@link BeanPostProcessor} which invokes {@link MuleRegistryHelper#postObjectRegistrationActions(Object)}
 * after spring finishes initialization over each object, only as long as the
 * {@link #applicationContext} is already running
 *
 * @since 3.7.0
 */
public class PostRegistrationActionsPostProcessor implements BeanPostProcessor
{

    private final ConfigurableApplicationContext applicationContext;
    private final MuleRegistryHelper registryHelper;

    public PostRegistrationActionsPostProcessor(ConfigurableApplicationContext applicationContext, MuleRegistryHelper registryHelper)
    {
        this.applicationContext = applicationContext;
        this.registryHelper = registryHelper;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
    {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        if (applicationContext.isRunning())
        {
            registryHelper.postObjectRegistrationActions(bean);
        }
        return bean;
    }
}
