/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.routing;

import org.mule.config.i18n.Message;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;

/**
 * <code>ResponseTimeoutException</code> is thrown when a response is not received
 * in a given timeout in the Response Router.
 * 
 * @see org.mule.umo.routing.UMOResponseRouter
 */
public class ResponseTimeoutException extends RoutingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6882278747922113239L;

    public ResponseTimeoutException(Message message, UMOMessage umoMessage, UMOEndpoint endpoint)
    {
        super(message, umoMessage, endpoint);
    }

    public ResponseTimeoutException(Message message,
                                    UMOMessage umoMessage,
                                    UMOEndpoint endpoint,
                                    Throwable cause)
    {
        super(message, umoMessage, endpoint, cause);
    }
}
