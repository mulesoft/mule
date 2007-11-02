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

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.endpoint.DynamicEndpointURIEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;
import org.mule.umo.routing.RoutePathNotFoundException;
import org.mule.umo.routing.RoutingException;
import org.mule.util.TemplateParser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The template endpoint router allows endpoints to be alered at runtime based on
 * properties set on the current event of fallback values set on the endpoint properties.
 * Templated values are expressed using square braces around a property name i.e.
 * axis:http://localhost:8082/MyService?method=[SOAP_METHOD] Note that Ant style property
 * templates cannot be used in valid URI strings, so we must use Square braces instead.
 */
public class TemplateEndpointRouter extends FilteringOutboundRouter
{

    // We used Square templates as they can exist as part of an uri.
    private TemplateParser parser = TemplateParser.createSquareBracesStyleParser();

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous)
        throws RoutingException
    {
        UMOMessage result = null;
        if (endpoints == null || endpoints.size() == 0)
        {
            throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), message, null);
        }
        try
        {
            UMOImmutableEndpoint ep = (UMOImmutableEndpoint) endpoints.get(0);
            String uri = ep.getEndpointURI().toString();
            if (logger.isDebugEnabled())
            {
                logger.debug("Uri before parsing is: " + uri);
            }
            Map props = new HashMap();
            // Also add the endpoint propertie so that users can set fallback values
            // when the property is not set on the event
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
            UMOEndpointURI newUri = new MuleEndpointURI(uri);
            if (!newUri.getScheme().equalsIgnoreCase(ep.getEndpointURI().getScheme()))
            {
                throw new CouldNotRouteOutboundMessageException(CoreMessages.schemeCannotChangeForRouter(
                    ep.getEndpointURI().getScheme(), newUri.getScheme()), message, ep);
            }
            ep = new DynamicEndpointURIEndpoint(ep, new MuleEndpointURI(uri));
            if (synchronous)
            {
                result = send(session, message, ep);
            }
            else
            {
                dispatch(session, message, ep);
            }
        }
        catch (UMOException e)
        {
            throw new CouldNotRouteOutboundMessageException(message, (UMOImmutableEndpoint) endpoints.get(0), e);
        }
        return result;
    }

}
