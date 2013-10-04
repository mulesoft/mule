/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
