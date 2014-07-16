/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.routing;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
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

    public RoutePathNotFoundException(MuleEvent event, MessageProcessor target)
    {
        super(event, target);
    }

    public RoutePathNotFoundException(MuleEvent event, MessageProcessor target, Throwable cause)
    {
        super(event, target, cause);
    }

    public RoutePathNotFoundException(Message message, MuleEvent event, MessageProcessor target)
    {
        super(message, event, target);
    }

    public RoutePathNotFoundException(Message message, MuleEvent event, MessageProcessor target,
        Throwable cause)
    {
        super(message, event, target, cause);
    }
}
