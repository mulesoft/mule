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
 * <code>CouldNotRouteOutboundMessageException</code> thrown if Mule fails to route
 * the current outbound event.
 */

public class CouldNotRouteOutboundMessageException extends RoutingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 4609966704030524483L;

    public CouldNotRouteOutboundMessageException(MuleEvent event, MessageProcessor target)
    {
        super(event, target);
    }

    public CouldNotRouteOutboundMessageException(MuleEvent event, MessageProcessor target, Throwable cause)
    {
        super(event, target, cause);
    }

    public CouldNotRouteOutboundMessageException(Message message, MuleEvent event, MessageProcessor target)
    {
        super(message, event, target);
    }

    public CouldNotRouteOutboundMessageException(Message message, MuleEvent event,
                                                 MessageProcessor target, Throwable cause)
    {
        super(message, event, target, cause);
    }
}
