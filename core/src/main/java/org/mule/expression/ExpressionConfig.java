/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
 * The {@link #getFullExpression(ExpressionManager)} will return the evaluator and expression 
 * information in a format that can be passed into the {@link DefaultExpressionManager}
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
    private volatile boolean parsed = false;

    public ExpressionConfig()
    {
        super();
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

    public void parse(String expressionString)
    {
        if (parsed)
        {
            return;
        }

        synchronized (this)
        {
            if (parsed)
            {
                return;
            }

            doParse(expressionString);

            parsed = true;
        } 
    }

    private void doParse(String expressionString)
    {
        if(expressionString.startsWith(expressionPrefix))
        {
            expressionString = expressionString.substring(expressionPrefix.length());
            expressionString = expressionString.substring(0, expressionString.length() - expressionPostfix.length());
        }

        int i = expressionString.indexOf(EXPRESSION_SEPARATOR);
        if(i < 0)
        {
            throw new IllegalArgumentException("Expression is invalid: " + expressionString);
        }
        this.evaluator = expressionString.substring(0, i);
        this.expression = expressionString.substring(i+1);
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
            fullExpression = expressionPrefix + evaluator + EXPRESSION_SEPARATOR + expression + expressionPostfix;
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
        fullExpression=null;
    }

    public String getEvaluator()
    {
        return evaluator;
    }

    public void setEvaluator(String evaluator)
    {
        this.evaluator = StringUtils.trimToNull(evaluator);
        fullExpression=null;
    }

    public String getExpression()
    {
        return expression;
    }

    public void setExpression(String expression)
    {
        this.expression = StringUtils.trimToEmpty(expression);
        this.fullExpression=null;
        this.parsed = false;
    }
}
