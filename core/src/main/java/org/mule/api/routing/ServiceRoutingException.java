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
import org.mule.api.service.Service;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;

/**
 * <code>ServiceRoutingException</code> is thrown due to a routing exception
 * between the endpoint the event was received on and the service receiving the
 * event.
 */
public class ServiceRoutingException extends RoutingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -113944443831267318L;

    private transient Service service;

    public ServiceRoutingException(Message message,
                                     MuleMessage muleMessage,
                                     ImmutableEndpoint endpoint,
                                     Service service)
    {
        super(generateMessage(message, endpoint, service), muleMessage, endpoint);
        this.service = service;
    }

    public ServiceRoutingException(Message message,
                                     MuleMessage muleMessage,
                                     ImmutableEndpoint endpoint,
                                     Service service,
                                     Throwable cause)
    {
        super(generateMessage(message, endpoint, service), muleMessage, endpoint, cause);
        this.service = service;
    }

    public ServiceRoutingException(MuleMessage message,
                                     ImmutableEndpoint endpoint,
                                     Service service)
    {
        super(generateMessage(null, endpoint, service), message, endpoint);
        this.service = service;
    }

    public ServiceRoutingException(MuleMessage message,
                                     ImmutableEndpoint endpoint,
                                     Service service,
                                     Throwable cause)
    {
        super(generateMessage(null, endpoint, service), message, endpoint, cause);
        this.service = service;

    }

    private static Message generateMessage(Message message,
                                           ImmutableEndpoint endpoint,
                                           Service service)
    {
        Message m = CoreMessages.routingFailedOnEndpoint(service, endpoint);
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

    public Service getService()
    {
        return service;
    }
}
