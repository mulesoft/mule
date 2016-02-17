/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.routing.RoutingException;
import org.mule.api.routing.TransformingMatchable;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.TemplateParser;

import java.util.LinkedList;
import java.util.List;

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
        return routes.get(index);
    }

    public boolean isUseTemplates()
    {
        return useTemplates;
    }

    public void setUseTemplates(boolean useTemplates)
    {
        this.useTemplates = useTemplates;
    }
    
    public boolean isTransformBeforeMatch()
    {
        return !transformers.isEmpty();
    }

}
