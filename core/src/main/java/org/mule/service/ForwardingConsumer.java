/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.service;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.routing.RoutingException;
import org.mule.api.service.Service;
import org.mule.routing.MessageFilter;

/**
 * <code>ForwardingConsumer</code> is used to forward an incoming event over another transport without
 * invoking a service. This can be used to implement a bridge accross different transports.
 * @deprecated
 */
public class ForwardingConsumer extends MessageFilter
{
    @Override
    public MuleEvent processNext(MuleEvent event) throws MessagingException
    {
        if (!(event.getFlowConstruct() instanceof Service))
        {
            throw new UnsupportedOperationException("ForwardingConsumer is only supported with Service");
        }

        OutboundRouterCollection router = ((Service) event.getFlowConstruct()).getOutboundRouter();

        // Set the stopFurtherProcessing flag to true to inform the
        // DefaultInboundRouterCollection not to route these events to the service
        event.setStopFurtherProcessing(true);

        if (router == null)
        {
            logger.debug("Descriptor has no outbound router configured to forward to, continuing with normal processing");
            return event;
        }
        else
        {
            try
            {
                MuleEvent resultEvent = router.process(event);
                if (resultEvent != null)
                {
                    return resultEvent;
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
}
