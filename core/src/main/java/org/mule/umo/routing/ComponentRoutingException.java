/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.routing;

import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

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

    private transient UMOComponent component;

    public ComponentRoutingException(Message message,
                                     UMOMessage umoMessage,
                                     UMOImmutableEndpoint endpoint,
                                     UMOComponent component)
    {
        super(generateMessage(message, endpoint, component), umoMessage, endpoint);
        this.component = component;
    }

    public ComponentRoutingException(Message message,
                                     UMOMessage umoMessage,
                                     UMOImmutableEndpoint endpoint,
                                     UMOComponent component,
                                     Throwable cause)
    {
        super(generateMessage(message, endpoint, component), umoMessage, endpoint, cause);
        this.component = component;
    }

    public ComponentRoutingException(UMOMessage umoMessage,
                                     UMOImmutableEndpoint endpoint,
                                     UMOComponent component)
    {
        super(generateMessage(null, endpoint, component), umoMessage, endpoint);
        this.component = component;
    }

    public ComponentRoutingException(UMOMessage umoMessage,
                                     UMOImmutableEndpoint endpoint,
                                     UMOComponent component,
                                     Throwable cause)
    {
        super(generateMessage(null, endpoint, component), umoMessage, endpoint, cause);
        this.component = component;

    }

    private static Message generateMessage(Message message,
                                           UMOImmutableEndpoint endpoint,
                                           UMOComponent component)
    {

        Message m = CoreMessages.routingFailedOnEndpoint(component.getDescriptor().getName(), 
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

    public UMOComponent getComponent()
    {
        return component;
    }
}
