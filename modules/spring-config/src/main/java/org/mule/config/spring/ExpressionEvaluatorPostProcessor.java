/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.expression.ExpressionManager;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Registers custom declarative expression evaluators configured via Spring config.
 *
 * @see ExpressionEvaluator
 */
public class ExpressionEvaluatorPostProcessor implements BeanPostProcessor
{
    private MuleContext muleContext;

    public ExpressionEvaluatorPostProcessor(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
    {
        if (bean instanceof ExpressionEvaluator)
        {
            ExpressionEvaluator ee = (ExpressionEvaluator) bean;

            final ExpressionManager expressionManager = muleContext.getExpressionManager();
            if (!expressionManager.isEvaluatorRegistered(ee.getName()))
            {
                expressionManager.registerEvaluator(ee);
            }
        }
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        return bean;
    }

}
