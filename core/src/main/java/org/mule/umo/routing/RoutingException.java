/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.routing;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * <code>RoutingException</code> is a base class for all routing exceptions.
 * Routing exceptions are only thrown for InboundMessageRouter and
 * OutboundMessageRouter and deriving types. Mule itself does not throw routing
 * exceptions when routing internal events.
 * 
 * @version $Revision$
 */
public class RoutingException extends MessagingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 2478458847072048645L;

    protected transient UMOImmutableEndpoint endpoint;

    public RoutingException(UMOMessage message, UMOImmutableEndpoint endpoint)
    {
        super(generateMessage(null, endpoint), message);
        this.endpoint = endpoint;
    }

    public RoutingException(UMOMessage umoMessage, UMOImmutableEndpoint endpoint, Throwable cause)
    {
        super(generateMessage(null, endpoint), umoMessage, cause);
        this.endpoint = endpoint;
    }

    public RoutingException(Message message, UMOMessage umoMessage, UMOImmutableEndpoint endpoint)
    {
        super(generateMessage(message, endpoint), umoMessage);
        this.endpoint = endpoint;
    }

    public RoutingException(Message message,
                            UMOMessage umoMessage,
                            UMOImmutableEndpoint endpoint,
                            Throwable cause)
    {
        super(generateMessage(message, endpoint), umoMessage, cause);
        this.endpoint = endpoint;
    }

    public UMOImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

    private static Message generateMessage(Message message, UMOImmutableEndpoint endpoint)
    {
        Message m = new Message(Messages.FAILED_TO_ROUTER_VIA_ENDPOINT, endpoint);
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
