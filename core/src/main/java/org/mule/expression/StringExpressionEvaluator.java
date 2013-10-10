/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

}
