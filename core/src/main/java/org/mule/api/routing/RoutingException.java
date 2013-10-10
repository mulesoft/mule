/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.routing;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
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

    protected final transient MessageProcessor route;

    public RoutingException(MuleEvent event, MessageProcessor route)
    {
        super(generateMessage(null, route), event);
        this.route = route;
    }

    public RoutingException(MuleEvent event, MessageProcessor route, Throwable cause)
    {
        super(generateMessage(null, route), event, cause);
        this.route = route;
    }

    public RoutingException(Message message, MuleEvent event, MessageProcessor route)
    {
        super(generateMessage(message, route), event);
        this.route = route;
    }

    public RoutingException(Message message, MuleEvent event, MessageProcessor route, Throwable cause)
    {
        super(generateMessage(message, route), event, cause);
        this.route = route;
    }

    public MessageProcessor getRoute()
    {
        return route;
    }

    private static Message generateMessage(Message message, MessageProcessor target)
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
