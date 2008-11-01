/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.expression.ExpressionManager;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.StringUtils;

/**
 * A simple configuration object for holding the common Expression evaluator configuration.
 * The {@link #getFullExpression()} will return the evaluator and expression information in a format
 * that can be passed into the {@link DefaultExpressionManager}
 */
public class ExpressionConfig
{
    public static final String CUSTOM_EVALUATOR = "custom";
    public static final String EXPRESSION_SEPARATOR = ":";
    private String expression;

    private String evaluator;

    private String customEvaluator;

    private String fullExpression;

    private String expressionPrefix = ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
    private String expressionPostfix = ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;

    public ExpressionConfig()
    {

    }

    public ExpressionConfig(String expression, String evaluator, String customEvaluator)
    {
        this(expression, evaluator, customEvaluator,
                ExpressionManager.DEFAULT_EXPRESSION_PREFIX,
                ExpressionManager.DEFAULT_EXPRESSION_POSTFIX);

    }

    public ExpressionConfig(String expression, String evaluator, String customEvaluator, String expressionPrefix, String expressionPostfix)
    {
        setCustomEvaluator(customEvaluator);
        setEvaluator(evaluator);
        setExpression(expression);
        this.expressionPostfix = expressionPostfix;
        this.expressionPrefix = expressionPrefix;
    }

    public void validate(ExpressionManager manager)
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

        if (!manager.isEvaluatorRegistered(evaluator))
        {
            throw new IllegalArgumentException(CoreMessages.expressionEvaluatorNotRegistered(evaluator).getMessage());
        }
    }

    public String getFullExpression(ExpressionManager manager)
    {
        if (fullExpression == null)
        {
            validate(manager);
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
        this.customEvaluator = StringUtils.trimToNull(customEvaluator);
    }

    public String getEvaluator()
    {
        return evaluator;
    }

    public void setEvaluator(String evaluator)
    {
        this.evaluator = StringUtils.trimToNull(evaluator);
    }

    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        this.expression = StringUtils.trimToNull(expression);
    }
}
