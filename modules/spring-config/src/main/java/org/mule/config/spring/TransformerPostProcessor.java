/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.transformer.Transformer;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Registers custom {@link Transformer} configured via Spring config.
 */
public class TransformerPostProcessor implements BeanPostProcessor
{

    private final MuleContext muleContext;

    public TransformerPostProcessor(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
    {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        if (bean instanceof Transformer)
        {
            try
            {
                muleContext.getRegistry().registerTransformer((Transformer) bean);
            }
            catch (MuleException e)
            {
                throw new BeanInitializationException("Error registering transformer", e);
            }
        }
        return bean;
    }
}
