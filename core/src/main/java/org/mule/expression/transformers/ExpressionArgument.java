/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression.transformers;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.expression.ExpressionConfig;

/**
 * TODO
 */
public class ExpressionArgument implements MuleContextAware
{
    private ExpressionConfig expressionConfig = new ExpressionConfig();
    private String name;
    private boolean optional;

    private MuleContext muleContext;

    public ExpressionArgument()
    {
    }

    public ExpressionArgument(String name, ExpressionConfig expressionConfig, boolean optional)
    {
        this.expressionConfig = expressionConfig;
        this.name = name;
        this.optional = optional;
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public ExpressionConfig getExpressionConfig()
    {
        return expressionConfig;
    }

    public void setExpressionConfig(ExpressionConfig expressionConfig)
    {
        this.expressionConfig = expressionConfig;
    }

    public boolean isOptional()
    {
        return optional;
    }

    public void setOptional(boolean optional)
    {
        this.optional = optional;
    }

    protected String getFullExpression()
    {
        return expressionConfig.getFullExpression(muleContext.getExpressionManager());
    }

    protected void validate()
    {
        expressionConfig.validate(muleContext.getExpressionManager());
    }

    public Object evaluate(MuleMessage message)
    {
        return muleContext.getExpressionManager().evaluate(getExpression(), getEvaluator(), message, isOptional());
    }

    public String getExpression()
    {
        return expressionConfig.getExpression();
    }

    public void setExpression(String expression)
    {
        expressionConfig.setExpression(expression);
    }

    public String getEvaluator()
    {
        return expressionConfig.getEvaluator();
    }
    
    public void setEvaluator(String evaluator)
    {
        expressionConfig.setEvaluator(evaluator);
    }

    public void setCustomEvaluator(String evaluator)
    {
        expressionConfig.setCustomEvaluator(evaluator);
    }

    public String getCustomEvaluator()
    {
        return expressionConfig.getCustomEvaluator();
    }


}
