/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
