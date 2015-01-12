/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.processors;

import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public class LifecyclePostProcessor implements BeanPostProcessor
{

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
    {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        try
        {
            if (bean instanceof Initialisable)
            {
                ((Initialisable) bean).initialise();
            }

            if (bean instanceof Startable) {
                ((Startable) bean).start();
            }

        } catch (Exception e) {
            throw new FatalBeanException("Exception found applying lifecycle", e);
        }

        return bean;
    }
}
