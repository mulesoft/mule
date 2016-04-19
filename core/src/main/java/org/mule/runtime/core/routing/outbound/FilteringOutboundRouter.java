/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.outbound;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.endpoint.EndpointException;
import org.mule.runtime.core.api.endpoint.EndpointURI;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.expression.ExpressionManager;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.runtime.core.api.routing.RoutePathNotFoundException;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.api.routing.TransformingMatchable;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.endpoint.DynamicURIOutboundEndpoint;
import org.mule.runtime.core.endpoint.MuleEndpointURI;
import org.mule.runtime.core.routing.AbstractRoutingStrategy;
import org.mule.runtime.core.util.TemplateParser;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <code>FilteringRouter</code> is a router that accepts events based on a filter
 * set.
 */

public class FilteringOutboundRouter extends AbstractOutboundRouter implements TransformingMatchable
{
    protected ExpressionManager expressionManager;

    private List<Transformer> transformers = new LinkedList<Transformer>();

    private Filter filter;

    private boolean useTemplates = true;
    
    // We used Square templates as they can exist as part of an URI.
    private TemplateParser parser = TemplateParser.createSquareBracesStyleParser();

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        expressionManager = muleContext.getExpressionManager();
    }

    @Override
    public MuleEvent route(MuleEvent event) throws RoutingException
    {
        MuleEvent result;

        MuleMessage message = event.getMessage();

        if (routes == null || routes.size() == 0)
        {
            throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), event, null);
        }

        MessageProcessor ep = getRoute(0, event);

        try
        {
            result = sendRequest(event, createEventToRoute(event, message), ep, true);
        }
        catch (RoutingException e)
        {
            throw e;
        }
        catch (MuleException e)
        {
            throw new CouldNotRouteOutboundMessageException(event, ep, e);
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

    @Override
    public boolean isMatch(MuleMessage message) throws MuleException
    {
        if (getFilter() == null)
        {
            return true;
        }

        message = muleContext.getTransformationService().applyTransformers(message, null, transformers);

        return getFilter().accept(message);
    }

    public List<Transformer> getTransformers()
    {
        return transformers;
    }

    public void setTransformers(List<Transformer> transformers)
    {
        this.transformers = transformers;
    }

    @Override
    public synchronized void addRoute(MessageProcessor target) throws MuleException
    {
        if (!useTemplates)
        {
            if (target instanceof ImmutableEndpoint)
            {
                ImmutableEndpoint endpoint = (ImmutableEndpoint) target;
                if (parser.isContainsTemplate(endpoint.getEndpointURI().toString()))
                {
                    useTemplates = true;
                }
            }
        }
        super.addRoute(target);
    }

    /**
     * Will Return the target at the given index and will resolve any template tags
     * on the Endpoint URI if necessary
     * 
     * @param index the index of the endpoint to get
     * @param event the current event. This is required if template matching is
     *            being used
     * @return the endpoint at the index, with any template tags resolved
     * @throws CouldNotRouteOutboundMessageException if the template causs the
     *             endpoint to become illegal or malformed
     */
    public MessageProcessor getRoute(int index, MuleEvent event) throws CouldNotRouteOutboundMessageException
    {
        if (!useTemplates)
        {
            return routes.get(index);
        }
        else
        {
            MuleMessage message = event.getMessage();
            MessageProcessor mp = routes.get(index);
            if (!(mp instanceof ImmutableEndpoint))
            {
                return routes.get(index);
            }
            OutboundEndpoint ep = (OutboundEndpoint) mp;
            String uri = ep.getAddress();

            if (logger.isDebugEnabled())
            {
                logger.debug("Uri before parsing is: " + uri);
            }

            AbstractRoutingStrategy.propagateMagicProperties(message, message);

            if (!parser.isContainsTemplate(uri))
            {
                logger.debug("Uri does not contain template(s)");
                return ep;
            }
            else
            {
                Map<String, Object> props = new HashMap<String, Object>();
                // Also add the endpoint properties so that users can set fallback values
                // when the property is not set on the event
                props.putAll(ep.getProperties());
                for (String propertyKey : message.getOutboundPropertyNames())
                {
                    Object value = message.getOutboundProperty(propertyKey);
                    props.put(propertyKey, value);
                }

                String newUriString = parser.parse(props, uri);
                if (parser.isContainsTemplate(newUriString))
                {
                    newUriString = this.getMuleContext().getExpressionManager().parse(newUriString, event, true);
                }
                if (logger.isDebugEnabled())
                {
                    logger.debug("Uri after parsing is: " + uri);
                }
                try
                {
                    EndpointURI newUri = new MuleEndpointURI(newUriString, muleContext);
                    EndpointURI endpointURI = ep.getEndpointURI();
                    if (endpointURI != null && !newUri.getScheme().equalsIgnoreCase(endpointURI.getScheme()))
                    {
                        throw new CouldNotRouteOutboundMessageException(
                                CoreMessages.schemeCannotChangeForRouter(ep.getEndpointURI().getScheme(),
                                        newUri.getScheme()),
                                event, ep);
                    }
                    newUri.initialise();

                    return new DynamicURIOutboundEndpoint(ep, newUri);
                }
                catch (EndpointException e)
                {
                    throw new CouldNotRouteOutboundMessageException(
                            CoreMessages.templateCausedMalformedEndpoint(uri, newUriString), event, ep, e);
                }
                catch (InitialisationException e)
                {
                    throw new CouldNotRouteOutboundMessageException(
                            CoreMessages.templateCausedMalformedEndpoint(uri, newUriString), event, ep, e);
                }
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
    
    @Override
    public boolean isTransformBeforeMatch()
    {
        return !transformers.isEmpty();
    }

}
