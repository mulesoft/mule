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
import org.mule.api.component.Component;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;

/**
 * <code>ComponentRoutingException</code> is thrown due to a routing exception
 * between the endpoint the event was received on and the component receiving the
 * event.
 */
public class ComponentRoutingException extends RoutingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -113944443831267318L;

    private transient Component component;

    public ComponentRoutingException(Message message,
                                     MuleMessage umoMessage,
                                     ImmutableEndpoint endpoint,
                                     Component component)
    {
        super(generateMessage(message, endpoint, component), umoMessage, endpoint);
        this.component = component;
    }

    public ComponentRoutingException(Message message,
                                     MuleMessage umoMessage,
                                     ImmutableEndpoint endpoint,
                                     Component component,
                                     Throwable cause)
    {
        super(generateMessage(message, endpoint, component), umoMessage, endpoint, cause);
        this.component = component;
    }

    public ComponentRoutingException(MuleMessage umoMessage,
                                     ImmutableEndpoint endpoint,
                                     Component component)
    {
        super(generateMessage(null, endpoint, component), umoMessage, endpoint);
        this.component = component;
    }

    public ComponentRoutingException(MuleMessage umoMessage,
                                     ImmutableEndpoint endpoint,
                                     Component component,
                                     Throwable cause)
    {
        super(generateMessage(null, endpoint, component), umoMessage, endpoint, cause);
        this.component = component;

    }

    private static Message generateMessage(Message message,
                                           ImmutableEndpoint endpoint,
                                           Component component)
    {

        Message m = CoreMessages.routingFailedOnEndpoint(component.getName(), 
            endpoint.getEndpointURI());
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

    public Component getComponent()
    {
        return component;
    }
}
