/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.umo.routing;

import org.mule.umo.routing.RoutingException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.UMOMessage;
import org.mule.config.i18n.Message;

/**
 * <code>ResponseTimeoutException</code> is thrown when a response is not received
 * in a given timeout in the Response Router.
 *
 * @see org.mule.umo.routing.UMOResponseRouter
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class ResponseTimeoutException extends RoutingException
{
    public ResponseTimeoutException(Message message, UMOMessage umoMessage, UMOEndpoint endpoint)
    {
        super(message, umoMessage, endpoint);
    }

    public ResponseTimeoutException(Message message, UMOMessage umoMessage,UMOEndpoint endpoint, Throwable cause)
    {
        super(message, umoMessage, endpoint, cause);
    }
}
