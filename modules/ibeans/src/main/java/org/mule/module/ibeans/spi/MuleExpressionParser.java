/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
