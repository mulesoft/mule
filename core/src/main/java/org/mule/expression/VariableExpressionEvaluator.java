/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

    /**
     * {@inheritDoc}
     */
    public void setName(String name)
    {
        throw new UnsupportedOperationException();
    }
}
