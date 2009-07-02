/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

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
import org.mule.util.TemplateParser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The TemplateEndpointRouter allows endpoints to be altered at runtime based on
 * properties set on the current event or fallback values set on the endpoint properties.
 * Templated values are expressed using square braces around a property name, i.e.
 * axis:http://localhost:8082/MyService?method=[SOAP_METHOD]. Note that Ant style property
 * templates cannot be used in valid URI strings, so we must use square braces instead.
 */
public class TemplateEndpointRouter extends FilteringOutboundRouter
{

    // We used square templates as they can exist as part of an URI.
    private TemplateParser parser = TemplateParser.createSquareBracesStyleParser();

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
            String uri = ep.getEndpointURI().toString();

            if (logger.isDebugEnabled())
            {
                logger.debug("Uri before parsing is: " + uri);
            }

            // Also add the endpoint properties so that users can set fallback values
            // when the property is not set on the event
            Map props = new HashMap();
            props.putAll(ep.getProperties());

            for (Iterator iterator = message.getPropertyNames().iterator(); iterator.hasNext();)
            {
                String propertyKey = (String) iterator.next();
                props.put(propertyKey, message.getProperty(propertyKey));
            }

            uri = parser.parse(props, uri);

            if (logger.isDebugEnabled())
            {
                logger.debug("Uri after parsing is: " + uri);
            }

            EndpointURI newUri = new MuleEndpointURI(uri, muleContext);

            if (!newUri.getScheme().equalsIgnoreCase(ep.getEndpointURI().getScheme()))
            {
                throw new CouldNotRouteOutboundMessageException(CoreMessages.schemeCannotChangeForRouter(
                    ep.getEndpointURI().getScheme(), newUri.getScheme()), message, ep);
            }

            ep = new DynamicURIOutboundEndpoint(ep, new MuleEndpointURI(uri, muleContext));

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
