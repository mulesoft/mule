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

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;

/**
 * <code>MalformedEndpointException</code> is thrown by the MuleEndpointURI class
 * if it fails to parse a Url
 * 
 * @see org.mule.impl.endpoint.MuleEndpointURI
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
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
        super(new Message(Messages.ENPOINT_X_IS_MALFORMED, endpoint));
    }

    /**
     * @param endpoint the endpoint that could not be parsed
     */
    public MalformedEndpointException(Message message, String endpoint)
    {
        super(new Message(Messages.ENPOINT_X_IS_MALFORMED, endpoint).setNextMessage(message));
    }

    /**
     * @param endpoint the endpoint that could not be parsed
     * @param cause the exception that cause this exception to be thrown
     */
    public MalformedEndpointException(String endpoint, Throwable cause)
    {
        super(new Message(Messages.ENPOINT_X_IS_MALFORMED, endpoint), cause);
    }

    public MalformedEndpointException(Throwable cause)
    {
        super(cause);
    }
}
