/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.processors;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.service.Service;
import org.mule.config.processors.DecoratingAnnotatedServiceProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * TODO
 */
public class DecoratingFlowAnnotationsBeanProcessor implements BeanPostProcessor, MuleContextAware
{
    private DecoratingAnnotatedServiceProcessor processor;

    public void setMuleContext(MuleContext muleContext)
    {
        processor = new DecoratingAnnotatedServiceProcessor(muleContext);
    }

    public Object postProcessBeforeInitialization(java.lang.Object o, java.lang.String s) throws BeansException
    {
        if(o instanceof Service)
        {
            return processor.process(o);
        }
        return o;
    }


    public Object postProcessAfterInitialization(java.lang.Object o, java.lang.String s) throws BeansException
    {
        return o;
    }
}
