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
 * A simple configuration object for holding the common Expression evaluator configuration. The
 * {@link #getFullExpression(ExpressionManager)} will return the evaluator and expression information in a
 * format that can be passed into the {@link DefaultExpressionManager}
 */
public class ExpressionConfig
{
    public static final String CUSTOM_EVALUATOR = "custom";
    public static final String EXPRESSION_SEPARATOR = ":";
    private String unParsedExpression;
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
        this(expression, evaluator, customEvaluator, ExpressionManager.DEFAULT_EXPRESSION_PREFIX,
            ExpressionManager.DEFAULT_EXPRESSION_POSTFIX);

    }

    public ExpressionConfig(String expression,
                            String evaluator,
                            String customEvaluator,
                            String expressionPrefix,
                            String expressionPostfix)
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
        if (expressionString.startsWith(expressionPrefix) && evaluator == null)
        {
            expressionString = expressionString.substring(expressionPrefix.length());
            expressionString = expressionString.substring(0,
                expressionString.length() - expressionPostfix.length());
        }

        int i = expressionString.indexOf(EXPRESSION_SEPARATOR);
        if (i >= 0 && evaluator == null)
        {
            // Attempt to work out if the expression uses an evaluator. This doesn't catch all cases, any
            // other cases will be caught during validation.
            String candidateEvaluator = expressionString.substring(0, i);
            if (!candidateEvaluator.matches("^[\\w-_]+$"))
            {
                this.expression = expressionString;
            }
            else
            {
                this.evaluator = candidateEvaluator;
                this.expression = expressionString.substring(i + 1);
            }
        }
        else
        {
            this.expression = expressionString;
        }
    }

    public void validate(ExpressionManager manager)
    {
        if (expression == null)
        {
            parse(unParsedExpression);
        }
        if (expression == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("expression").getMessage());
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

        if (evaluator != null && !manager.isEvaluatorRegistered(evaluator))
        {
            throw new IllegalArgumentException(CoreMessages.expressionEvaluatorNotRegistered(evaluator)
                .getMessage());
        }
    }

    public String getFullExpression(ExpressionManager manager)
    {
        if (fullExpression == null)
        {
            if (expression == null)
            {
                parse(unParsedExpression);
            }
            validate(manager);
            if (evaluator != null)
            {
                fullExpression = expressionPrefix + evaluator + EXPRESSION_SEPARATOR + expression
                                 + expressionPostfix;
            }
            else
            {
                fullExpression = expressionPrefix + expression + expressionPostfix;
            }
        }
        return fullExpression;
    }

    public String getCustomEvaluator()
    {
        if (expression == null)
        {
            parse(unParsedExpression);
        }
        return customEvaluator;
    }

    public void setCustomEvaluator(String customEvaluator)
    {
        this.customEvaluator = StringUtils.trimToNull(customEvaluator);
        fullExpression = null;
    }

    public String getEvaluator()
    {
        if (expression == null)
        {
            parse(unParsedExpression);
        }
        return evaluator;
    }

    public void setEvaluator(String evaluator)
    {
        this.evaluator = StringUtils.trimToNull(evaluator);
        fullExpression = null;
    }

    public String getExpression()
    {
        if (expression == null)
        {
            parse(unParsedExpression);
        }
        return expression;
    }

    public void setExpression(String expression)
    {
        this.unParsedExpression = expression;
        this.expression = null;
        this.fullExpression = null;
        this.parsed = false;
    }
}
