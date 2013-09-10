/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.endpoint;

import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;

/**
 * <code>EndpointNotFoundException</code> is thrown when an endpoint name or
 * protocol is specified but a matching endpoint is not registered with the Mule
 * server
 */

public class EndpointNotFoundException extends EndpointException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 790450139906970837L;

    public EndpointNotFoundException(String endpoint)
    {
        super(CoreMessages.endpointNotFound(endpoint));
    }

    /**
     * @param message the exception message
     */
    public EndpointNotFoundException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public EndpointNotFoundException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public EndpointNotFoundException(Throwable cause)
    {
        super(cause);
    }
}
