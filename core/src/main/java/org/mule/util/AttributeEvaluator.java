/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;

import java.util.regex.Pattern;

/**
 * This class acts as a wrapper for configuration attributes that support simple text, expression or regular
 * expressions. It can be extended to support other cases too.
 */
public class AttributeEvaluator
{
    private static final Pattern SINGLE_EXPRESSION_REGEX_PATTERN = Pattern.compile("^#\\[(?:(?!#\\[).)*\\]$");

    private enum AttributeType
    {
        EXPRESSION, STRING
    }

    private final String attributeValue;
    private ExpressionManager expressionManager;
    private AttributeType attributeType;

    public AttributeEvaluator(String attributeValue)
    {
        this.attributeValue = attributeValue;
    }

    public void initialize(final ExpressionManager expressionManager)
    {
        this.expressionManager = expressionManager;
        resolveAttributeType();
    }

    private void resolveAttributeType()
    {
        if (isSingleExpression(attributeValue))
        {
            this.attributeType = AttributeType.EXPRESSION;
        }
        else
        {
            this.attributeType = AttributeType.STRING;
        }
    }

    private boolean isSingleExpression(String expression)
    {
        return expression != null && SINGLE_EXPRESSION_REGEX_PATTERN.matcher(expression).matches();
    }

    public boolean isExpression()
    {
        return attributeType.equals(AttributeType.EXPRESSION);
    }

    public boolean isString()
    {
        return attributeType.equals(AttributeType.STRING);
    }

    public Object resolveValue(MuleMessage message)
    {
        if (isExpression())
        {
            return expressionManager.evaluate(attributeValue, message);
        }
        else
        {
            return expressionManager.parse(attributeValue, message);
        }
    }

    public String getRawValue()
    {
        return attributeValue;
    }
}
