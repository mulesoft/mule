/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
