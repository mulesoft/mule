/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Responsible for passing in the MuleContext instance for all objects in the
 * registry that want it. For an object to get an instance of the MuleContext
 * it must implement MuleContextAware.
 * 
 * @see MuleContextAware
 * @see org.mule.api.MuleContext
 */
public class MuleContextPostProcessor implements BeanPostProcessor
{
    private MuleContext muleContext;

    public MuleContextPostProcessor(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
    {
        if (bean instanceof MuleContextAware)
        {
            if (muleContext == null)
            {
                return bean;
            }

            ((MuleContextAware) bean).setMuleContext(muleContext);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        return bean;
    }
}
