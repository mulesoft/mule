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

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.routing.UMOOutboundRouterCollection;

/**
 * <code>ForwardingConsumer</code> is used to forward an incoming event over
 * another transport without invoking a component. This can be used to implement a
 * bridge accross different transports.
 */
public class ForwardingConsumer extends SelectiveConsumer
{

    public UMOEvent[] process(UMOEvent event) throws MessagingException
    {
        if (super.process(event) != null)
        {
            UMOOutboundRouterCollection router = event.getComponent().getOutboundRouter();

            // Set the stopFurtherProcessing flag to true to inform the
            // InboundRouterCollection not to route these events to the component
            event.setStopFurtherProcessing(true);

            if (router == null)
            {
                logger.debug("Descriptor has no outbound router configured to forward to, continuing with normal processing");
                return new UMOEvent[]{event};
            }
            else
            {
                try
                {
                    UMOMessage message = new MuleMessage(event.getTransformedMessage(), event.getMessage());

                    UMOMessage response = router.route(message, event.getSession(), event.isSynchronous());
                    // TODO What's the correct behaviour for async endpoints?
                    // maybe let router.route() return a Future for the returned msg?
                    if (response != null)
                    {
                        return new UMOEvent[]{new MuleEvent(response, event)};
                    }
                    else
                    {
                        return null;
                    }

                }
                catch (UMOException e)
                {
                    throw new RoutingException(event.getMessage(), event.getEndpoint(), e);
                }
            }
        }
        return null;
    }
}
