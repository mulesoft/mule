/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.expression;

import org.mule.config.i18n.CoreMessages;

/**
 * Is thrown explicitly when an expression is Malformed or invalid. Malformed means the syntax is not correct,
 * but an expression can be invalid if it refers to an expression namespace or function that does not exist
 */
public class InvalidExpressionException extends ExpressionRuntimeException
{
    private String expression;

    private String message;

    public InvalidExpressionException(String expression, String message)
    {
        super(CoreMessages.createStaticMessage(message + ". Offending expression string is: " + expression));
        this.expression = expression;
        this.message = message;
    }

    public String getExpression()
    {
        return expression;
    }

    public String getMessage()
    {
        return message;
    }

}
