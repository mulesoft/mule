/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transport;

import org.mule.api.endpoint.EndpointException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;

/**
 * <code>NoReceiverForEndpointException</code> is thrown when an enpoint is
 * specified for a receiver but no such receiver exists.
 */

public class NoReceiverForEndpointException extends EndpointException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3954838511333933643L;

    /**
     * @param endpoint the endpoint that could not be located
     */
    public NoReceiverForEndpointException(String endpoint)
    {
        super(CoreMessages.endpointNotFound(endpoint));
    }

    /**
     * @param message the exception message
     */
    public NoReceiverForEndpointException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public NoReceiverForEndpointException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public NoReceiverForEndpointException(Throwable cause)
    {
        super(cause);
    }

}
