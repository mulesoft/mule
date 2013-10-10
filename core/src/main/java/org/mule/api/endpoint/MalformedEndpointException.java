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
 * <code>MalformedEndpointException</code> is thrown by the MuleEndpointURI class
 * if it fails to parse a Url
 * 
 * @see org.mule.endpoint.MuleEndpointURI
 */

public class MalformedEndpointException extends EndpointException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3179045414716505094L;

    /**
     * @param endpoint the endpoint that could not be parsed
     */
    public MalformedEndpointException(String endpoint)
    {
        super(CoreMessages.endpointIsMalformed(endpoint));
    }

    /**
     * @param endpoint the endpoint that could not be parsed
     */
    public MalformedEndpointException(Message message, String endpoint)
    {
        super(CoreMessages.endpointIsMalformed(endpoint).setNextMessage(message));
    }

    /**
     * @param endpoint the endpoint that could not be parsed
     * @param cause the exception that cause this exception to be thrown
     */
    public MalformedEndpointException(String endpoint, Throwable cause)
    {
        super(CoreMessages.endpointIsMalformed(endpoint), cause);
    }

    public MalformedEndpointException(Throwable cause)
    {
        super(cause);
    }
}
