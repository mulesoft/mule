/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.routing;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.config.i18n.Message;

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

    public RoutePathNotFoundException(MuleMessage message, ImmutableEndpoint endpoint)
    {
        super(message, endpoint);
    }

    public RoutePathNotFoundException(MuleMessage message, ImmutableEndpoint endpoint, Throwable cause)
    {
        super(message, endpoint, cause);
    }

    public RoutePathNotFoundException(Message message, MuleMessage muleMessage, ImmutableEndpoint endpoint)
    {
        super(message, muleMessage, endpoint);
    }

    public RoutePathNotFoundException(Message message,
                                      MuleMessage muleMessage,
                                      ImmutableEndpoint endpoint,
                                      Throwable cause)
    {
        super(message, muleMessage, endpoint, cause);
    }
}
