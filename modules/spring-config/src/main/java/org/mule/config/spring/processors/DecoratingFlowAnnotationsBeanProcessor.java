/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
