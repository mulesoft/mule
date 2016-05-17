/*
 *(c) 2003-2016 MuleSoft, Inc. This software is protected under international copyright
 *law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 *(or other master license agreement) separately entered into in writing between you and
 *MuleSoft. If such an agreement is not in place, you may not use the software.
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
