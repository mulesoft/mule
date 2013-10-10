/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
