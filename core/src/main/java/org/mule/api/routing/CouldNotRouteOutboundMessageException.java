/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.routing;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
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

    /**
     * @deprecated use CouldNotRouteOutboundMessageException(MuleEvent, RoutingTarget)
     */
    @Deprecated
    public CouldNotRouteOutboundMessageException(MuleMessage message, MessageProcessor target)
    {
        super(message, target);
    }

    public CouldNotRouteOutboundMessageException(MuleEvent event, MessageProcessor target)
    {
        super(event, target);
    }

    /**
     * @deprecated use CouldNotRouteOutboundMessageException(MuleEvent, RoutingTarget, Throwable)
     */
    @Deprecated
    public CouldNotRouteOutboundMessageException(MuleMessage muleMessage, MessageProcessor target, Throwable cause)
    {
        super(muleMessage, target, cause);
    }

    public CouldNotRouteOutboundMessageException(MuleEvent event, MessageProcessor target, Throwable cause)
    {
        super(event, target, cause);
    }

    /**
     * @deprecated use CouldNotRouteOutboundMessageException(Message, MuleEvent, RoutingTarget)
     */
    @Deprecated
    public CouldNotRouteOutboundMessageException(Message message, MuleMessage muleMessage, MessageProcessor target)
    {
        super(message, muleMessage, target);
    }

    public CouldNotRouteOutboundMessageException(Message message, MuleEvent event, MessageProcessor target)
    {
        super(message, event, target);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public CouldNotRouteOutboundMessageException(Message message, MuleMessage muleMessage,
                                                 MessageProcessor target, Throwable cause)
    {
        super(message, muleMessage, target, cause);
    }

    public CouldNotRouteOutboundMessageException(Message message, MuleEvent event,
                                                 MessageProcessor target, Throwable cause)
    {
        super(message, event, target, cause);
    }
}
