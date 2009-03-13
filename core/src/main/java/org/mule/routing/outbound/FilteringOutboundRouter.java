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
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.routing.RoutingException;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.DynamicURIOutboundEndpoint;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.util.TemplateParser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <code>FilteringRouter</code> is a router that accepts events based on a filter
 * set.
 */

public class FilteringOutboundRouter extends AbstractOutboundRouter
{
    private List transformers = new LinkedList();

    private Filter filter;

    private boolean useTemplates = false;

    // We used Square templates as they can exist as part of an URI.
    private TemplateParser parser = TemplateParser.createSquareBracesStyleParser();

    protected ExpressionManager expressionManager;

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        expressionManager = muleContext.getExpressionManager();
    }

    public MuleMessage route(MuleMessage message, MuleSession session)
        throws RoutingException
    {
        MuleMessage result = null;

        if (endpoints == null || endpoints.size() == 0)
        {
            throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), message, null);
        }

        OutboundEndpoint ep = getEndpoint(0, message);

        try
        {
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
            throw new CouldNotRouteOutboundMessageException(message, ep, e);
        }
        return result;
    }

    public Filter getFilter()
    {
        return filter;
    }

    public void setFilter(Filter filter)
    {
        this.filter = filter;
    }

    public boolean isMatch(MuleMessage message) throws RoutingException
    {
        if (getFilter() == null)
        {
            return true;
        }
        try
        {
            message.applyTransformers(transformers);
        }
        catch (TransformerException e)
        {
            throw new RoutingException(
                    CoreMessages.transformFailedBeforeFilter(),
                    message, (ImmutableEndpoint) endpoints.get(0), e);
        }
        return getFilter().accept(message);
    }

    public List getTransformers()
    {
        return transformers;
    }

    public void setTransformers(List transformers)
    {
        this.transformers = transformers;
    }

    public void addEndpoint(OutboundEndpoint endpoint)
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
    public OutboundEndpoint getEndpoint(int index, MuleMessage message)
        throws CouldNotRouteOutboundMessageException
    {
        if (!useTemplates)
        {
            return (OutboundEndpoint) endpoints.get(index);
        }
        else
        {
            OutboundEndpoint ep = (OutboundEndpoint) endpoints.get(index);
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
                EndpointURI newUri = new MuleEndpointURI(newUriString);
                if (!newUri.getScheme().equalsIgnoreCase(ep.getEndpointURI().getScheme()))
                {
                    throw new CouldNotRouteOutboundMessageException(CoreMessages.schemeCannotChangeForRouter(
                        ep.getEndpointURI().getScheme(), newUri.getScheme()), message, ep);
                }

                return new DynamicURIOutboundEndpoint(ep, newUri);
            }
            catch (EndpointException e)
            {
                throw new CouldNotRouteOutboundMessageException(
                    CoreMessages.templateCausedMalformedEndpoint(uri, newUriString), 
                    message, ep, e);
            }
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
    
    public boolean isRequiresNewMessage()
    {
        return transformers.size() > 1;
    }

}
