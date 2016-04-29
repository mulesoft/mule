/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing.outbound;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.runtime.core.api.routing.RoutePathNotFoundException;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.routing.AbstractRoutingStrategy;
import org.mule.runtime.core.routing.CorrelationMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a router that sequentially routes a given message to the list of
 * registered endpoints and returns the aggregate responses as the result.
 * Aggregate response is built using the partial responses obtained from
 * synchronous endpoints.
 * The routing process can be stopped after receiving a partial response.
 */
public abstract class AbstractSequenceRouter extends FilteringOutboundRouter
{

    @Override
    public MuleEvent route(MuleEvent event) throws RoutingException
    {
        MuleMessage message = event.getMessage();

        if (routes == null || routes.size() == 0)
        {
            throw new RoutePathNotFoundException(CoreMessages.noEndpointsForRouter(), event, null);
        }
        if (enableCorrelation != CorrelationMode.NEVER)
        {
            boolean correlationSet = message.getCorrelationId() != null;
            if (correlationSet && (enableCorrelation == CorrelationMode.IF_NOT_SET))
            {
                logger.debug("CorrelationId is already set, not setting Correlation group size");
            }
            else
            {
                // the correlationId will be set by the AbstractOutboundRouter
                message.setCorrelationGroupSize(routes.size());
            }
        }

        List<MuleEvent> results = new ArrayList<MuleEvent>(routes.size());
        try
        {
            for (int i = 0; i < routes.size(); i++)
            {
                MessageProcessor mp = getRoute(i, event);
                OutboundEndpoint endpoint = mp instanceof OutboundEndpoint ? (OutboundEndpoint) mp : null;
                if (endpoint == null || endpoint.getFilter() == null || (endpoint.getFilter() != null && endpoint.getFilter().accept(message)))
                {
                    AbstractRoutingStrategy.validateMessageIsNotConsumable(event, message);
                    MuleMessage clonedMessage = cloneMessage(event, message);

                    MuleEvent result = sendRequest(event, createEventToRoute(event, clonedMessage), mp, true);
                    if (result != null && !VoidMuleEvent.getInstance().equals(result))
                    {
                        results.add(result);
                    }
                    // AbstractRoutingStrategy.validateMessageIsNotConsumable(event,message);
                    // MuleMessage clonedMessage = cloneMessage(event, message);

                    if (!continueRoutingMessageAfter(result))
                    {
                        break;
                    }
                    // MuleEvent result = sendRequest(event, createEventToRoute(event, clonedMessage), mp, true);
                    // if (result != null && !VoidMuleEvent.getInstance().equals(result))
                    // {
                    // results.add(result);
                    // }
                    //
                    // if (!continueRoutingMessageAfter(result))
                    // {
                    // break;
                    // }
                }
            }
        }
        catch (MuleException e)
        {
            throw new CouldNotRouteOutboundMessageException(event, routes.get(0), e);
        }
        return resultsHandler.aggregateResults(results, event, muleContext);
    }


    /**
     * Lets subclasses decide if the routing of a given message should continue
     * or not after receiving a given response from a synchronous endpoint.
     *
     * @param response the last received response
     * @return true if must continue and false otherwise.
     * @throws MuleException when the router should stop processing throwing an
     *                       exception without returning any results to the caller.
     */
    protected abstract boolean continueRoutingMessageAfter(MuleEvent response) throws MuleException;
}
