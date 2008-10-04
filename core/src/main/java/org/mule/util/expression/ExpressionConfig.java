/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.expression;

import org.mule.config.i18n.CoreMessages;

/**
 * A simple configuration object for holding the common Expression evaluator configuration.
 * The {@link #getFullExpression()} will return the evaluator and expression information in a format
 * that can be passed into the {@link org.mule.util.expression.ExpressionEvaluatorManager}
 */
public class ExpressionConfig
{
    public static final String CUSTOM_EVALUATOR = "custom";
    public static final String EXPRESSION_SEPARATOR = ":";
    private String expression;

    private String evaluator;

    private String customEvaluator;

    private String fullExpression;

    private String expressionPrefix = ExpressionEvaluatorManager.DEFAULT_EXPRESSION_PREFIX;
    private String expressionPostfix = ExpressionEvaluatorManager.DEFAULT_EXPRESSION_POSTFIX;

    public ExpressionConfig()
    {
    }

    public ExpressionConfig(String expression, String evaluator, String customEvaluator)
    {
        this.customEvaluator = customEvaluator;
        this.evaluator = evaluator;
        this.expression = expression;
    }

    public ExpressionConfig(String expression, String evaluator, String customEvaluator, String expressionPrefix, String expressionPostfix)
    {
        this.customEvaluator = customEvaluator;
        this.evaluator = evaluator;
        this.expression = expression;
        this.expressionPostfix = expressionPostfix;
        this.expressionPrefix = expressionPrefix;
        this.fullExpression = fullExpression;
    }

    public void validate()
    {
        if (expression == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("expression").getMessage());
        }
        if (evaluator == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("evaluator").getMessage());
        }
        if (CUSTOM_EVALUATOR.equalsIgnoreCase(evaluator))
        {
            if (customEvaluator == null)
            {
                throw new IllegalArgumentException(CoreMessages.objectIsNull("custom evaluator").getMessage());
            }
            else
            {
                evaluator = customEvaluator;
            }
        }

        if (!ExpressionEvaluatorManager.isEvaluatorRegistered(evaluator))
        {
            throw new IllegalArgumentException(CoreMessages.expressionEvaluatorNotRegistered(evaluator).getMessage());
        }
    }

    public String getFullExpression()
    {
        if (fullExpression == null)
        {
            fullExpression = expressionPrefix + evaluator + ":" + expression + expressionPostfix;
        }
        return fullExpression;
    }

    public String getCustomEvaluator()
    {
        return customEvaluator;
    }

    public void setCustomEvaluator(String customEvaluator)
    {
        this.customEvaluator = customEvaluator;
    }

    public String getEvaluator()
    {
        return evaluator;
    }

    public void setEvaluator(String evaluator)
    {
        this.evaluator = evaluator;
    }

    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        this.expression = expression;
    }
}
