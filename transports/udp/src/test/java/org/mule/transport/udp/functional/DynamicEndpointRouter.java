/*
 * $Id: TemplateEndpointRouter.java 10961 2008-02-22 19:01:02Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.udp.functional;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.routing.RoutingException;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.DynamicURIOutboundEndpoint;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.routing.outbound.FilteringOutboundRouter;

import java.util.Iterator;

/**
 * The DynamicEndpointRouter allows endpoints to be altered at runtime based on
 * properties set on the current event or fallback values set on the endpoint
 * properties. Templated values are expressed using square braces around a property
 * name, i.e. axis:http://localhost:8082/MyService?method=[SOAP_METHOD]. Note that
 * Ant style property templates cannot be used in valid URI strings, so we must use
 * square braces instead.
 */
public class DynamicEndpointRouter extends FilteringOutboundRouter
{

    public MuleMessage route(MuleMessage message, MuleSession session)
        throws RoutingException
    {
        MuleMessage result = null;

        if (endpoints == null || endpoints.size() == 0)
        {
            throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), message, null);
        }

        try
        {
            OutboundEndpoint ep = (OutboundEndpoint) endpoints.get(0);
            EndpointURI newUri;

            for (Iterator iterator = message.getPropertyNames().iterator(); iterator.hasNext();)
            {
                String propertyKey = (String) iterator.next();
                Object propertyValue = message.getProperty(propertyKey);
                if (propertyKey.equalsIgnoreCase("packet.port"))
                {
                    newUri = new MuleEndpointURI("udp://localhost:" + ((Integer) propertyValue).intValue(), muleContext);
                    if (logger.isDebugEnabled())
                    {
                        logger.info("Uri after parsing is: " + newUri.getAddress());
                    }
                    ep = new DynamicURIOutboundEndpoint(ep, newUri);
                    break;
                }
            }

            if (ep.isSynchronous())
            {
                result = send(session, message, ep);
            }
            else
            {
                dispatch(session, message, ep);
            }
        }
        catch (MuleException e)
        {
            throw new CouldNotRouteOutboundMessageException(message, (ImmutableEndpoint) endpoints.get(0), e);
        }

        return result;

    }

}
