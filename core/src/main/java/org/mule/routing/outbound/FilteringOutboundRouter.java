/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleMessage;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.umo.UMOException;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;
import org.mule.umo.routing.RoutePathNotFoundException;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.TemplateParser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <code>FilteringRouter</code> is a router that accepts events based on a filter
 * set.
 */

public class FilteringOutboundRouter extends AbstractOutboundRouter
{
    private UMOTransformer transformer;

    private UMOFilter filter;

    private boolean useTemplates = false;

    // We used Square templates as they can exist as part of an URI.
    private TemplateParser parser = TemplateParser.createSquareBracesStyleParser();

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous)
        throws RoutingException
    {
        UMOMessage result = null;

        if (endpoints == null || endpoints.size() == 0)
        {
            throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), message, null);
        }

        UMOEndpoint ep = getEndpoint(0, message);

        try
        {
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
            throw new CouldNotRouteOutboundMessageException(message, ep, e);
        }
        return result;
    }

    public UMOFilter getFilter()
    {
        return filter;
    }

    public void setFilter(UMOFilter filter)
    {
        this.filter = filter;
    }

    public boolean isMatch(UMOMessage message) throws RoutingException
    {
        if (getFilter() == null)
        {
            return true;
        }
        if (transformer != null)
        {
            try
            {
                Object payload = transformer.transform(message.getPayload());
                message = new MuleMessage(payload, message);
            }
            catch (TransformerException e)
            {
                throw new RoutingException(
                    CoreMessages.transformFailedBeforeFilter(),
                    message, (UMOEndpoint) endpoints.get(0), e);
            }
        }
        return getFilter().accept(message);
    }

    public UMOTransformer getTransformer()
    {
        return transformer;
    }

    public void setTransformer(UMOTransformer transformer)
    {
        this.transformer = transformer;
    }

    public void addEndpoint(UMOEndpoint endpoint)
    {
        if (!useTemplates && parser.isContainsTemplate(endpoint.getEndpointURI().toString()))
        {
            useTemplates = true;
        }
        super.addEndpoint(endpoint);
    }

    /**
     * Will Return the endpont at the given index and will resolve any template tags
     * on the Endpoint URI if necessary
     * 
     * @param index the index of the endpoint to get
     * @param message the current message. This is required if template matching is
     *            being used
     * @return the endpoint at the index, with any template tags resolved
     * @throws CouldNotRouteOutboundMessageException if the template causs the
     *             endpoint to become illegal or malformed
     */
    public UMOEndpoint getEndpoint(int index, UMOMessage message)
        throws CouldNotRouteOutboundMessageException
    {
        if (!useTemplates)
        {
            return (UMOEndpoint) endpoints.get(index);
        }
        else
        {
            UMOEndpoint ep = (UMOEndpoint) endpoints.get(index);
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
            String newUriString = parser.parse(props, uri);
            if (logger.isDebugEnabled())
            {
                logger.debug("Uri after parsing is: " + uri);
            }
            try
            {
                UMOEndpointURI newUri = new MuleEndpointURI(newUriString);
                if (!newUri.getScheme().equalsIgnoreCase(ep.getEndpointURI().getScheme()))
                {
                    throw new CouldNotRouteOutboundMessageException(
                        CoreMessages.schemeCannotChangeForRouter(ep.getEndpointURI().getScheme(),
                        newUri.getScheme()), message, ep);
                }
                ep.setEndpointURI(newUri);
            }
            catch (EndpointException e)
            {
                throw new CouldNotRouteOutboundMessageException(
                    CoreMessages.templateCausedMalformedEndpoint(uri, newUriString), 
                    message, ep, e);
            }

            return ep;
        }
    }

    public boolean isUseTemplates()
    {
        return useTemplates;
    }

    public void setUseTemplates(boolean useTemplates)
    {
        this.useTemplates = useTemplates;
    }
}
