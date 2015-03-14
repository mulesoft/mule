/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.processors;

import java.beans.PropertyDescriptor;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.ApplicationContext;

/**
 * Specialization of {@link AutowiredAnnotationBeanPostProcessor} which only considers
 * beans which are defined on a given {@link ApplicationContext}. This is useful
 * to avoid exceptions related to unsatisfied dependencies when using parent context
 * which also define a {@link AutowiredAnnotationBeanPostProcessor}
 *
 * @since 3.7.0
 */
public class ContextExclusiveInjectorProcessor extends AutowiredAnnotationBeanPostProcessor
{

    private ApplicationContext applicationContext;

    public ContextExclusiveInjectorProcessor(ApplicationContext applicationContext)
    {
        this.applicationContext = applicationContext;
    }

    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException
    {
        if (applicationContext.containsBean(beanName))
        {
            return super.postProcessPropertyValues(pvs, pds, bean, beanName);
        }

        return pvs;
    }
}
