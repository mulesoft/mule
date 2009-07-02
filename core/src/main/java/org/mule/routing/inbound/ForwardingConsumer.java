/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.inbound;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.routing.RoutingException;

/**
 * <code>ForwardingConsumer</code> is used to forward an incoming event over
 * another transport without invoking a service. This can be used to implement a
 * bridge accross different transports.
 */
public class ForwardingConsumer extends SelectiveConsumer
{

    public MuleEvent[] process(MuleEvent event) throws MessagingException
    {
        if (super.process(event) != null)
        {
            OutboundRouterCollection router = event.getService().getOutboundRouter();

            // Set the stopFurtherProcessing flag to true to inform the
            // DefaultInboundRouterCollection not to route these events to the service
            event.setStopFurtherProcessing(true);

            if (router == null)
            {
                logger.debug("Descriptor has no outbound router configured to forward to, continuing with normal processing");
                return new MuleEvent[]{event};
            }
            else
            {
                try
                {
                    MuleMessage message = new DefaultMuleMessage(event.transformMessage(), event.getMessage(), muleContext);

                    MuleMessage response = router.route(message, event.getSession());
                    // TODO What's the correct behaviour for async endpoints?
                    // maybe let router.route() return a Future for the returned msg?
                    if (response != null)
                    {
                        return new MuleEvent[]{new DefaultMuleEvent(response, event)};
                    }
                    else
                    {
                        return null;
                    }

                }
                catch (MuleException e)
                {
                    throw new RoutingException(event.getMessage(), event.getEndpoint(), e);
                }
            }
        }
        return null;
    }
}
