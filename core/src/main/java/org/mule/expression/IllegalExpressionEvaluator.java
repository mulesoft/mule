/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.transformer.types.TypedValue;

/**
 * Defines an {@link ExpressionEvaluator} that can never be evaluated.
 */
public abstract class IllegalExpressionEvaluator implements ExpressionEvaluator
{

    public static final String ILLEGAL_EVALUATOR_MESSAGE = "Evaluator %s can not be used in this context";

    @Override
    public Object evaluate(String expression, MuleMessage message)
    {
        throw new UnsupportedOperationException(String.format(ILLEGAL_EVALUATOR_MESSAGE, getName()));
    }

    @Override
    public TypedValue evaluateTyped(String expression, MuleMessage message)
    {
        throw new UnsupportedOperationException(String.format(ILLEGAL_EVALUATOR_MESSAGE, getName()));
    }
}
