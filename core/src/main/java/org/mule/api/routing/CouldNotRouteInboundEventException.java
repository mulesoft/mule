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
 * <code>CouldNotRouteInboundEventException</code> thrown if the current service
 * cannot accept the inbound event.
 */

public class CouldNotRouteInboundEventException extends RoutingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 2736231899561051617L;

    /**
     * @deprecated use CouldNotRouteInboundEventException(MuleEvent, RoutingTarget)
     */
    @Deprecated
    public CouldNotRouteInboundEventException(MuleMessage message, RoutingTarget target)
    {
        super(message, target);
    }

    public CouldNotRouteInboundEventException(MuleEvent event, RoutingTarget target)
    {
        super(event, target);
    }

    /**
     * @deprecated use CouldNotRouteInboundEventException(MuleEvent, RoutingTarget, Throwable)
     */
    @Deprecated
    public CouldNotRouteInboundEventException(MuleMessage muleMessage, RoutingTarget target, Throwable cause)
    {
        super(muleMessage, target, cause);
    }

    public CouldNotRouteInboundEventException(MuleEvent event, RoutingTarget target, Throwable cause)
    {
        super(event, target, cause);
    }

    /**
     * @deprecated use CouldNotRouteInboundEventException(Message, MuleEvent, RoutingTarget)
     */
    @Deprecated
    public CouldNotRouteInboundEventException(Message message, MuleMessage muleMessage, RoutingTarget target)
    {
        super(message, muleMessage, target);
    }

    public CouldNotRouteInboundEventException(Message message, MuleEvent event, RoutingTarget target)
    {
        super(message, event, target);
    }

    /**
     * @deprecated use 
     */
    @Deprecated
    public CouldNotRouteInboundEventException(Message message, MuleMessage muleMessage,
        RoutingTarget target, Throwable cause)
    {
        super(message, muleMessage, target, cause);
    }

    public CouldNotRouteInboundEventException(Message message, MuleEvent event,
        RoutingTarget target, Throwable cause)
    {
        super(message, event, target, cause);
    }
}
