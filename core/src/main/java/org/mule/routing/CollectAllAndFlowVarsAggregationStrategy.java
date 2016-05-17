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
import org.mule.routing.CollectAllAggregationStrategy;
import org.mule.routing.DefaultRouterResultsHandler;
import org.mule.routing.CompositeRoutingException;
import org.mule.api.transformer.DataType;

/**
 * If no routes generated exeption then it returns a new {@link MuleEvent} under the
 * rules of {@link DefaultRouterResultsHandler} (you can change this behaviour by
 * overriding {@link #aggregateWithoutFailedRoutes(AggregationContext)}. Otherwise, a
 * {@link CompositeRoutingException} is thrown (override
 * {@link #aggregateWithFailedRoutes(AggregationContext) to customize}
 * 
 * @since 3.5.0
 */
public class CollectAllAndFlowVarsAggregationStrategy extends CollectAllAggregationStrategy
{

    protected AggregationContext copyFlowVariblesTogetOriginalEventInAggregationContext(AggregationContext context)
    {
    	MuleEvent originalEvent = context.getOriginalEvent();
    	for (MuleEvent event : context.getEvents())
    	{
    		for (String flowName : event.getFlowVariableNames())
    		{
    			originalEvent.setFlowVariable(flowName, event.getFlowVariable(flowName), event.getFlowVariableDataType(flowName));
    		}
    	}
    
    	return context;
    }
    
    protected MuleEvent aggregateWithoutFailedRoutes(AggregationContext context) throws MuleException
    {
    	context = copyFlowVariblesTogetOriginalEventInAggregationContext(context);
    	return super.aggregateWithoutFailedRoutes(context);
    }



}
