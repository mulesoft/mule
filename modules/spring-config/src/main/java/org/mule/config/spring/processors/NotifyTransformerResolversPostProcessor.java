/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.processors;

import org.mule.api.registry.TransformerResolver;
import org.mule.api.transformer.Converter;
import org.mule.registry.MuleRegistryHelper;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class NotifyTransformerResolversPostProcessor implements BeanPostProcessor
{

    private final MuleRegistryHelper registry;

    public NotifyTransformerResolversPostProcessor(MuleRegistryHelper registry)
    {
        this.registry = registry;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
    {
        if (bean instanceof Converter)
        {
            registry.notifyTransformerResolvers((Converter) bean, TransformerResolver.RegistryAction.ADDED);
        }

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        return bean;
    }
}
