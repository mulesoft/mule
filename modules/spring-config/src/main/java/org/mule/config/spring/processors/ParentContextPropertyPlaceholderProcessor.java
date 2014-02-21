/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.processors;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.config.spring.SpringRegistry;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.context.ApplicationContext;

public class ParentContextPropertyPlaceholderProcessor implements MuleContextAware, BeanFactoryPostProcessor
{

    private MuleContext muleContext;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException
    {
        ApplicationContext applicationContext = muleContext.getRegistry().lookupObject(SpringRegistry.SPRING_APPLICATION_CONTEXT);
        ApplicationContext domainContext = applicationContext.getParent();
        if (domainContext != null)
        {
            Map<String, PlaceholderConfigurerSupport> propertySourcesPlaceholderConfigurerMap = domainContext.getBeansOfType(PlaceholderConfigurerSupport.class);
            for (PlaceholderConfigurerSupport propertySourcesPlaceholderConfigurer : propertySourcesPlaceholderConfigurerMap.values())
            {
                propertySourcesPlaceholderConfigurer.postProcessBeanFactory(beanFactory);
            }
        }
    }

    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

}
