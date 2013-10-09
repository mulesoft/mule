/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;

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

}
