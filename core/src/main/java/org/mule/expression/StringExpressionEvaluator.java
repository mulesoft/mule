/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.ExpressionEvaluator;

/**
 * Will process an expression string that can contain other expressions
 */
public class StringExpressionEvaluator implements ExpressionEvaluator, MuleContextAware
{
    public static final String NAME = "string";

    private MuleContext context;

    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }

    public Object evaluate(String expression, MuleMessage message)
    {
        return context.getExpressionManager().parse(expression, message);
    }

    /**
     * Gts the name of the object
     *
     * @return the name of the object
     */
    public String getName()
    {
        return NAME;
    }

    /**
     * Sets the name of the object
     *
     * @param name the name of the object
     */
    public void setName(String name)
    {
        throw new UnsupportedOperationException();
    }
}
