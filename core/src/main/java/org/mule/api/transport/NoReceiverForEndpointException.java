/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
