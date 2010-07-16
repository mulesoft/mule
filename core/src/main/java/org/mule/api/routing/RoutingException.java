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

import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;

/**
 * <code>RoutingException</code> is a base class for all routing exceptions.
 * Routing exceptions are only thrown for DefaultInboundRouterCollection and
 * DefaultOutboundRouterCollection and deriving types. Mule itself does not throw routing
 * exceptions when routing internal events.
 */
public class RoutingException extends MessagingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 2478458847072048645L;

    protected final transient RoutingTarget route;

    public RoutingException(MuleMessage message, RoutingTarget route)
    {
        super(generateMessage(null, route), message);
        this.route = route;
    }

    public RoutingException(MuleMessage message, RoutingTarget route, Throwable cause)
    {
        super(generateMessage(null, route), message, cause);
        this.route = route;
    }

    public RoutingException(Message message, MuleMessage muleMessage, RoutingTarget route)
    {
        super(generateMessage(message, route), muleMessage);
        this.route = route;
    }

    public RoutingException(Message message,
                            MuleMessage muleMessage,
                            RoutingTarget route,
                            Throwable cause)
    {
        super(generateMessage(message, route), muleMessage, cause);
        this.route = route;
    }

    public RoutingTarget getRoute()
    {
        return route;
    }

    private static Message generateMessage(Message message, RoutingTarget target)
    {
        Message m = CoreMessages.failedToRouterViaEndpoint(target);
        if (message != null)
        {
            message.setNextMessage(m);
            return message;
        }
        else
        {
            return m;
        }
    }
}
