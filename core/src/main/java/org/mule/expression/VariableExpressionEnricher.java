/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.expression;

import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionEnricher;
import org.mule.api.transport.PropertyScope;

public class VariableExpressionEnricher implements ExpressionEnricher
{

    public static final String NAME = "variable";

    public void enrich(String expression, MuleMessage message, Object object)
    {
        message.setProperty(expression, object, PropertyScope.INVOCATION);
    }

    public String getName()
    {
        return NAME;
    }

    public void setName(String name)
    {
        throw new UnsupportedOperationException();
    }

}
