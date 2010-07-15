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
 * <code>CouldNotRouteOutboundMessageException</code> thrown if Mule fails to route
 * the current outbound event.
 */

public class CouldNotRouteOutboundMessageException extends RoutingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 4609966704030524482L;

    public CouldNotRouteOutboundMessageException(MuleMessage message, RoutingTarget target)
    {
        super(message, target);
    }

    public CouldNotRouteOutboundMessageException(MuleMessage muleMessage, RoutingTarget target, Throwable cause)
    {
        super(muleMessage, target, cause);
    }

    public CouldNotRouteOutboundMessageException(Message message, MuleMessage muleMessage, RoutingTarget target)
    {
        super(message, muleMessage, target);
    }

    public CouldNotRouteOutboundMessageException(Message message,
                                                 MuleMessage muleMessage,
                                                 RoutingTarget target,
                                                 Throwable cause)
    {
        super(message, muleMessage, target, cause);
    }
}
