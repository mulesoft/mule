/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
