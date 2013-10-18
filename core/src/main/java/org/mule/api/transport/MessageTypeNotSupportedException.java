/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.transport;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.config.i18n.CoreMessages;

/**
 * <code>MessageTypeNotSupportedException</code> is thrown when a {@link MuleMessage} instance is 
 * to be created with an payload type that is not of supported type by that 
 * {@link MuleMessageFactory}.
 */
public class MessageTypeNotSupportedException extends MuleException
{
    private static final long serialVersionUID = -3954838511333933644L;

    public MessageTypeNotSupportedException(Object message, Class<?> creatorClass)
    {
        super(CoreMessages.messageNotSupportedByMuleMessageFactory(message, creatorClass));
    }

    public MessageTypeNotSupportedException(Object message, Class<?> creatorClass, Throwable cause)
    {
        super(CoreMessages.messageNotSupportedByMuleMessageFactory(message, creatorClass), cause);
    }
}
