/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.annotations.processors.spring;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.impl.annotations.processors.DirectBindAnnotationProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * A Spring post processor that wraps the Mule {@link org.mule.impl.annotations.processors.AnnotatedServiceObjectProcessor}. The
 * service is analyzed and created before the bean is initialized.
 */
public class BindAnnotationBeanPostProcessor implements BeanPostProcessor, MuleContextAware
{
    private DirectBindAnnotationProcessor bindProcessor = new DirectBindAnnotationProcessor();

    private MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
    {
        if (beanName.equals("foo"))
        {
            System.out.println("");
        }
        bindProcessor.setMuleContext(muleContext);
        return bindProcessor.process(bean);
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        return bean;
    }
}