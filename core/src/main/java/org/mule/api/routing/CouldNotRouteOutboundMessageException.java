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
