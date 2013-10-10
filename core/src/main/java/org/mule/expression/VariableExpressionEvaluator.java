/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;
import org.mule.api.transport.PropertyScope;

/**
 * Looks up the variable on the message using the name given.
 * 
 * @see ExpressionEvaluator
 * @see DefaultExpressionManager
 */
public class VariableExpressionEvaluator implements ExpressionEvaluator
{
    public static final String NAME = "variable";

    public Object evaluate(String expression, MuleMessage message)
    {
        // Variable is a shortcut for invocation properties
        return ExpressionUtils.getProperty(expression, PropertyScope.INVOCATION, message);
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return NAME;
    }

}
