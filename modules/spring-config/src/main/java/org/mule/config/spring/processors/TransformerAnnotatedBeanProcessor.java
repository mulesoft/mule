/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.processors;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.config.transformer.AnnotatedTransformerObjectProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * TODO
 */
public class TransformerAnnotatedBeanProcessor implements BeanPostProcessor, MuleContextAware
{
    private AnnotatedTransformerObjectProcessor processor;

    public void setMuleContext(MuleContext muleContext)
    {
        processor = new AnnotatedTransformerObjectProcessor(muleContext);
    }

    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException
    {
        return processor.process(o);
    }


    public Object postProcessAfterInitialization(Object o, String s) throws BeansException
    {
        return o;
    }
}
