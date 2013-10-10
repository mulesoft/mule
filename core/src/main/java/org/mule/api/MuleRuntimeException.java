/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api;

import org.mule.config.i18n.Message;

/**
 * <code>MuleRuntimeException</code> Is the base runtime exception type for the
 * Mule Server any other runtimes exceptions thrown by Mule code will use or be based
 * on this exception. Runtime exceptions in mule are only ever thrown where the
 * method is not declared to throw an exception and the exception is serious.
 */
public class MuleRuntimeException extends RuntimeException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6728041560892553159L;

    /**
     * @param message the exception message
     */
    public MuleRuntimeException(Message message)
    {
        super(message.getMessage());
    }

    /**
     * @param message the exception message
     * @param cause the exception that triggered this exception
     */
    public MuleRuntimeException(Message message, Throwable cause)
    {
        super(message.getMessage(), cause);
    }

    /**
     * @param cause the exception that triggered this exception
     */
    public MuleRuntimeException(Throwable cause)
    {
        super(cause);
    }
}
