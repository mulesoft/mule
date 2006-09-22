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
 * <code>EndpointNotFoundException</code> is thrown when an endpoint name or
 * protocol is specified but a matching endpoint is not registered with the Mule
 * server
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class EndpointNotFoundException extends EndpointException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 790450139906970837L;

    public EndpointNotFoundException(String endpoint)
    {
        super(new Message(Messages.ENDPOINT_X_NOT_FOUND, endpoint));
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
