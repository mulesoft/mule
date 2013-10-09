/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
