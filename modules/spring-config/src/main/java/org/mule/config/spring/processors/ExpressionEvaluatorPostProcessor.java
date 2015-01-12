/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.processors;

import org.mule.api.MuleContext;
import org.mule.api.expression.ExpressionEvaluator;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

public final class ExpressionEvaluatorPostProcessor implements BeanPostProcessor
{
    private final MuleContext muleContext;

    public ExpressionEvaluatorPostProcessor(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
    {
        if(bean instanceof ExpressionEvaluator)
        {
            muleContext.getExpressionManager().registerEvaluator((ExpressionEvaluator) bean);
        }

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        return bean;
    }
}
