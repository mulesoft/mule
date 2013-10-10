/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

/**
 * <code>MuleException</code> Is the base exception type for the Mule application
 * any other exceptions thrown by Mule code will be based on this exception.
 */
public class DefaultMuleException extends MuleException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 2554735072826262515L;

    public DefaultMuleException(String message)
    {
        this(MessageFactory.createStaticMessage(message));
    }

    /**
     * @param message the exception message
     */
    public DefaultMuleException(Message message)
    {
        super(message);
    }

    public DefaultMuleException(String message, Throwable cause)
    {
        this(MessageFactory.createStaticMessage(message), cause);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public DefaultMuleException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public DefaultMuleException(Throwable cause)
    {
        super(cause);
    }
}
