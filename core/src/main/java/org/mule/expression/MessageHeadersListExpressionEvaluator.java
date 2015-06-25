/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.MuleMessage;

import java.util.List;

/**
 * Looks up the property on the message using the expression given. The expression can contain a comma-separated list
 * of header names to lookup. A {@link java.util.List} of values is returned.
 *
 * @see MessageHeadersExpressionEvaluator
 * @see org.mule.api.expression.ExpressionEvaluator
 * @see DefaultExpressionManager
 */
public class MessageHeadersListExpressionEvaluator extends AbstractExpressionEvaluator
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
