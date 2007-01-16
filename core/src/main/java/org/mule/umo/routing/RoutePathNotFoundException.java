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
 * <code>RoutePathNotFoundException</code> is thrown if a routing path for an event
 * cannot be found. This can be caused if there is no (or no matching) endpoint for
 * the event to route through.
 */
public class RoutePathNotFoundException extends RoutingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -8481434966594513065L;

    public RoutePathNotFoundException(UMOMessage message, UMOEndpoint endpoint)
    {
        super(message, endpoint);
    }

    public RoutePathNotFoundException(UMOMessage umoMessage, UMOEndpoint endpoint, Throwable cause)
    {
        super(umoMessage, endpoint, cause);
    }

    public RoutePathNotFoundException(Message message, UMOMessage umoMessage, UMOEndpoint endpoint)
    {
        super(message, umoMessage, endpoint);
    }

    public RoutePathNotFoundException(Message message,
                                      UMOMessage umoMessage,
                                      UMOEndpoint endpoint,
                                      Throwable cause)
    {
        super(message, umoMessage, endpoint, cause);
    }
}
