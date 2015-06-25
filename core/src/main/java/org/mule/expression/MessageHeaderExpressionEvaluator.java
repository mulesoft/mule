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
 * Looks up the property on the message using the property name given.  If the call on the messgae returns null,
 * parameters on the inbound endpoint will also be checked.
 *
 * @see MessageHeadersListExpressionEvaluator
 * @see MessageHeadersExpressionEvaluator
 * @see ExpressionEvaluator
 * @see DefaultExpressionManager
 */
public class MessageHeaderExpressionEvaluator implements ExpressionEvaluator
{
    public static final String NAME = "header";

    public Object evaluate(String expression, MuleMessage message)
    {
        return ExpressionUtils.getPropertyWithScope(expression, message);
    }

    @Override
    public TypedValue evaluateTyped(String expression, MuleMessage message)
    {
        return ExpressionUtils.getTypedProperty(expression, message);
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return NAME;
    }

}
