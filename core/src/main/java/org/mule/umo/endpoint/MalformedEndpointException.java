/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.endpoint;

import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;

/**
 * <code>MalformedEndpointException</code> is thrown by the MuleEndpointURI class
 * if it fails to parse a Url
 * 
 * @see org.mule.impl.endpoint.MuleEndpointURI
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
