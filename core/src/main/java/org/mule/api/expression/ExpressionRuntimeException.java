/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.expression;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.Message;

/**
 * If thrown by the {@link org.mule.expression.DefaultExpressionManager} if an expression returns null
 * and failIfNull was set when {@link ExpressionManager#evaluate(String,org.mule.api.MuleMessage,boolean)}
 * was called.
 */
public class ExpressionRuntimeException extends MuleRuntimeException
{
    /**
     * @param message the exception message
     */
    public ExpressionRuntimeException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that triggered this exception
     */
    public ExpressionRuntimeException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
