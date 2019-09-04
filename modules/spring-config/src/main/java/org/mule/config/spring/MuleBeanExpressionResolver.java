/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import java.util.regex.Pattern;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.context.expression.StandardBeanExpressionResolver;

public class MuleBeanExpressionResolver extends StandardBeanExpressionResolver
{

    @Override
    public Object evaluate(String value, BeanExpressionContext evalContext) throws BeansException
    {
        /*
        Matches case the value being evaluated has an occurrence of #{, which is the expressionPrefix,
        but no occurrence of }, which is the expression suffix.
        */
        Pattern nonClosedExpressionSuffixCase = Pattern.compile("^.*#\\{[^}]*$");
        if (value != null && nonClosedExpressionSuffixCase.matcher(value).matches())
        {
            // Directly return value, since LiteralExpression(value).getValue() does that.
            return value;
        }
        return super.evaluate(value, evalContext);
    }
}
