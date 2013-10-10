/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
