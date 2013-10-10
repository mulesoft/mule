/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.expression;

import org.mule.config.i18n.Message;

/**
 * Is thrown explicitly when an expression is executed that returns a null value when a value is required.  Typically,
 * this exception will only be thrown by Mule-specific evaluators such as Header or Attachment.
 */
public class RequiredValueException extends ExpressionRuntimeException
{
    public RequiredValueException(Message message)
    {
        super(message);
    }

    public RequiredValueException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
