/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.processors;

import org.mule.api.MuleContext;
import org.mule.api.annotations.Transformer;
import org.mule.config.transformer.AnnotatedTransformerObjectProcessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Will check all method level annotations to see if there are any {@link Transformer} annotations present.
 * For each method annotated with {@link Transformer} a Mule transformer will be created.  When the
 * transformer is used, the method will get invoked
 *
 * @see Transformer
 * @since 3.7.0
 */
public class AnnotatedTransformerObjectPostProcessor implements BeanPostProcessor
{

    private AnnotatedTransformerObjectProcessor processor;

    public AnnotatedTransformerObjectPostProcessor(MuleContext muleContext)
    {
        processor = new AnnotatedTransformerObjectProcessor();
        processor.setMuleContext(muleContext);
    }

    @Override
    public Object postProcessBeforeInitialization(Object object, String beanName) throws BeansException
    {
        return processor.process(object);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        return bean;
    }
}
