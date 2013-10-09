/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.spi;

import org.mule.routing.filters.ExpressionFilter;

import org.ibeans.api.IBeansException;
import org.ibeans.spi.ErrorFilter;

/**
 * An implementation of a {@link org.ibeans.spi.ErrorFilter} that allows for error filter expressions to be configured on
 * an iBean
 */
public class ErrorExpressionFilter extends ExpressionFilter implements ErrorFilter<MuleResponseMessage>
{
    private String errorCodeExpression;

    public ErrorExpressionFilter(String evaluator, String customEvaluator, String expression, String errorCodeExpr)
    {
        super(evaluator, customEvaluator, expression);
        if (errorCodeExpr != null && errorCodeExpr.length() > 0)
        {
            this.errorCodeExpression = errorCodeExpr;
        }
    }

    public ErrorExpressionFilter(String evaluator, String expression, String errorCodeExpr)
    {
        super(evaluator, expression);
        if (errorCodeExpr != null && errorCodeExpr.length() > 0)
        {
            this.errorCodeExpression = errorCodeExpr;
        }
    }

    public ErrorExpressionFilter(String expression)
    {
        super(expression);
    }

    public ErrorExpressionFilter()
    {
        super();
    }

    public String getErrorCodeExpression()
    {
        return errorCodeExpression;
    }

    public String getErrorExpression()
    {
        return getExpression();
    }

    public String getType()
    {
        return getEvaluator();
    }

    public boolean accept(MuleResponseMessage object) throws IBeansException
    {
        return accept(object.getMessage());
    }
}
