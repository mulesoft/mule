/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class FlowRefFactoryBean implements FactoryBean<MessageProcessor>, ApplicationContextAware
{
    private String refName;
    private ApplicationContext applicationContext;

    public void setName(String name)
    {
        this.refName = name;
    }

    @Override
    public MessageProcessor getObject() throws Exception
    {
        final MessageProcessor processor = ((MessageProcessor) applicationContext.getBean(refName));
        if (processor instanceof FlowConstruct)
        {
            // If a FlowConstuct is reference then decouple lifcycle/injection
            return new MessageProcessor()
            {
                public MuleEvent process(MuleEvent event) throws MuleException
                {
                    return processor.process(event);
                }
            };
        }
        else
        {
            return processor;
        }
    }

    @Override
    public boolean isSingleton()
    {
        return false;
    }

    @Override
    public Class<?> getObjectType()
    {
        return MessageProcessor.class;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }
}
