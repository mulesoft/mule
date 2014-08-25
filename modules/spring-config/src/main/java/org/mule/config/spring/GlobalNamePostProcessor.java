/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.GlobalNameableObject;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Injects the bean name to beans implementing @{link GlobalNameableObject}
 */
public class GlobalNamePostProcessor implements BeanPostProcessor
{

    private static final String INNER_BEAN_PLACEHOLDER_PREFIX = "(inner bean)";

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
    {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        if (bean instanceof GlobalNameableObject && beanName!=null && !beanName.startsWith(INNER_BEAN_PLACEHOLDER_PREFIX))
        {
            ((GlobalNameableObject) bean).setGlobalName(beanName);
        }
        return bean;
    }
}
