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
import org.mule.api.endpoint.ImmutableEndpoint;
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

    protected final transient ImmutableEndpoint endpoint;

    public RoutingException(MuleMessage message, ImmutableEndpoint endpoint)
    {
        super(generateMessage(null, endpoint), message);
        this.endpoint = endpoint;
    }

    public RoutingException(MuleMessage message, ImmutableEndpoint endpoint, Throwable cause)
    {
        super(generateMessage(null, endpoint), message, cause);
        this.endpoint = endpoint;
    }

    public RoutingException(Message message, MuleMessage muleMessage, ImmutableEndpoint endpoint)
    {
        super(generateMessage(message, endpoint), muleMessage);
        this.endpoint = endpoint;
    }

    public RoutingException(Message message,
                            MuleMessage muleMessage,
                            ImmutableEndpoint endpoint,
                            Throwable cause)
    {
        super(generateMessage(message, endpoint), muleMessage, cause);
        this.endpoint = endpoint;
    }

    public ImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

    private static Message generateMessage(Message message, ImmutableEndpoint endpoint)
    {
        Message m = CoreMessages.failedToRouterViaEndpoint(endpoint);
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
