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

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
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
    private static final long serialVersionUID = -8481434966594513066L;

    /**
     * @deprecated use RoutePathNotFoundException(MuleEvent, RoutingTarget)
     */
    @Deprecated
    public RoutePathNotFoundException(MuleMessage message, RoutingTarget target)
    {
        super(message, target);
    }

    public RoutePathNotFoundException(MuleEvent event, RoutingTarget target)
    {
        super(event, target);
    }

    /**
     * @deprecated use RoutePathNotFoundException(MuleEvent, RoutingTarget, Throwable)
     */
    @Deprecated
    public RoutePathNotFoundException(MuleMessage message, RoutingTarget target, Throwable cause)
    {
        super(message, target, cause);
    }

    public RoutePathNotFoundException(MuleEvent event, RoutingTarget target, Throwable cause)
    {
        super(event, target, cause);
    }

    /**
     * @deprecated use RoutePathNotFoundException(Message, MuleEvent, RoutingTarget)
     */
    @Deprecated
    public RoutePathNotFoundException(Message message, MuleMessage muleMessage, RoutingTarget target)
    {
        super(message, muleMessage, target);
    }

    public RoutePathNotFoundException(Message message, MuleEvent event, RoutingTarget target)
    {
        super(message, event, target);
    }

    /**
     * @deprecated use 
     */
    @Deprecated
    public RoutePathNotFoundException(Message message, MuleMessage muleMessage, RoutingTarget target,
        Throwable cause)
    {
        super(message, muleMessage, target, cause);
    }

    public RoutePathNotFoundException(Message message, MuleEvent event, RoutingTarget target,
        Throwable cause)
    {
        super(message, event, target, cause);
    }
}
