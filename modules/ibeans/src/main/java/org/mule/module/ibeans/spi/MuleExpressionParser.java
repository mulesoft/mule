/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.spi;

import org.mule.api.MuleContext;

import org.ibeans.impl.support.AbstractExpressionParser;

/**
 * Implementation exposes the Mule {@link org.mule.api.expression.ExpressionManager} to evaluate expressions in {@link org.ibeans.annotation.Call} and
 * {@link org.ibeans.annotation.Template} annotations
 */
public class MuleExpressionParser extends AbstractExpressionParser<MuleRequestMessage, MuleResponseMessage>
{
    private MuleContext muleContext;

    public MuleExpressionParser(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public Object evaluate(String expression, MuleRequestMessage request)
    {
        return muleContext.getExpressionManager().parse(expression, request.getMessage());
    }

    public Object evaluate(String expression, MuleResponseMessage response)
    {
        return muleContext.getExpressionManager().evaluate(expression, response.getMessage());
    }

    public Object evaluate(String evaluator, String expression, MuleResponseMessage response)
    {
        return muleContext.getExpressionManager().evaluate(expression, evaluator, response.getMessage(), false);
    }
}
