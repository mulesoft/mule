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

public class MessageHeaderExpressionEnricher implements ExpressionEnricher
{

    public static final String NAME = "header";

    public void enrich(String expression, MuleMessage message, Object object)
    {
        String propertyName = expression;
        PropertyScope scope = ExpressionUtils.getScope(expression);
        if (scope != null)
        {
            // cut-off leading scope and separator
            propertyName = expression.substring(scope.getScopeName().length() + 1);
        }
        else
        {
            // default
            scope = PropertyScope.OUTBOUND;
        }

        message.setProperty(propertyName, object, scope);
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
