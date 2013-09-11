/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.udp.functional;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.DynamicURIOutboundEndpoint;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.routing.outbound.FilteringOutboundRouter;

public class DynamicEndpointRouter extends FilteringOutboundRouter
{
    @Override
    public MuleEvent route(MuleEvent event) throws RoutingException
    {
        MuleMessage message = event.getMessage();
        MuleEvent result;

        if (routes == null || routes.size() == 0)
        {
            throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), event, null);
        }

        try
        {
            MessageProcessor ep = routes.get(0);
            EndpointURI newUri;

            if (ep instanceof OutboundEndpoint)
            {
                for (String propertyKey : message.getOutboundPropertyNames())
                {
                    Object propertyValue = message.getOutboundProperty(propertyKey);
                    if (propertyKey.equalsIgnoreCase("packet.port"))
                    {
                        int port = (Integer) propertyValue;
                        newUri = new MuleEndpointURI("udp://localhost:" + port, muleContext);
                        if (logger.isDebugEnabled())
                        {
                            logger.info("Uri after parsing is: " + newUri.getAddress());
                        }
                        ep = new DynamicURIOutboundEndpoint((OutboundEndpoint) ep, newUri);
                        break;
                    }
                }
            }
            result = sendRequest(event, message, ep, true);
        }
        catch (MuleException e)
        {
            throw new CouldNotRouteOutboundMessageException(event, routes.get(0), e);
        }

        return result;
    }
}
