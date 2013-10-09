/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.expression;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEvaluator;

import java.util.List;

/**
 * Looks up the property on the message using the expression given. The expression can contain a comma-separated list
 * of header names to lookup. A {@link java.util.List} of values is returned.
 *
 * @see MessageHeadersExpressionEvaluator
 * @see org.mule.api.expression.ExpressionEvaluator
 * @see DefaultExpressionManager
 */
public class MessageHeadersListExpressionEvaluator implements ExpressionEvaluator
{
    public static final String NAME = "headers-list";

    public Object evaluate(String expression, MuleMessage message)
    {
        return ExpressionUtils.getPropertyWithScope(expression, message, List.class);
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return NAME;
    }

}
