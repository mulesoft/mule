/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.routing.AggregationContext;
import org.mule.api.routing.RouterResultsHandler;

/**
 * If no routes generated exeption then it returns a new {@link MuleEvent} under the
 * rules of {@link DefaultRouterResultsHandler} (you can change this behaviour by
 * overriding {@link #aggregateWithoutFailedRoutes(AggregationContext)}. Otherwise, a
 * {@link CompositeRoutingException} is thrown (override
 * {@link #aggregateWithFailedRoutes(AggregationContext) to customize}
 * 
 * @since 3.5.0
 */
public class CollectAllAggregationStrategy implements AggregationStrategy
{

    private RouterResultsHandler resultsHandler = new DefaultRouterResultsHandler();

    @Override
    public MuleEvent aggregate(AggregationContext context) throws MuleException
    {
        if (context.collectEventsWithExceptions().isEmpty())
        {
            return this.aggregateWithoutFailedRoutes(context);
        }
        else
        {
            return this.aggregateWithFailedRoutes(context);
        }
    }

    protected MuleEvent aggregateWithoutFailedRoutes(AggregationContext context) throws MuleException
    {

        return this.resultsHandler.aggregateResults(context.getEvents(), context.getOriginalEvent(),
            context.getOriginalEvent().getMuleContext());
    }

    protected MuleEvent aggregateWithFailedRoutes(AggregationContext context) throws MuleException
    {
        throw new CompositeRoutingException(context.getOriginalEvent(), context.collectRouteExceptions());
    }

}
