/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.endpoint;

import org.mule.api.MuleException;
import org.mule.config.i18n.Message;

/**
 * <code>EndpointException</code> is an abstract exception extended by endpoint
 * specific exceptions.
 */

public class EndpointException extends MuleException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 3219403251233216800L;

    /**
     * @param message the exception message
     */
    public EndpointException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public EndpointException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public EndpointException(Throwable cause)
    {
        super(cause);
    }
}
