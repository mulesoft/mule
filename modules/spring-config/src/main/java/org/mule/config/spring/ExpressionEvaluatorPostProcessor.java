/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
        if (muleContext == null)
        {
            return bean;
        }

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
