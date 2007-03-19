/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.routers;

import org.mule.routing.AggregationException;
import org.mule.routing.inbound.EventGroup;
import org.mule.routing.response.ResponseCorrelationAggregator;
import org.mule.umo.UMOMessage;
import org.mule.umo.routing.RoutingException;

/**
 * <code>BankQuotesInboundAggregator</code> receives a number of quotes and selects the
 * lowest
 */
public class BankQuotesResponseAggregator extends ResponseCorrelationAggregator
{
    /**
     * This method is invoked if the shouldAggregate method is called and returns
     * true. Once this method returns an aggregated message the event group is
     * removed from the router
     * 
     * @param events the event group for this request
     * @return an aggregated message
     * @throws org.mule.umo.routing.RoutingException if the aggregation fails. in
     *             this scenario the whole event group is removed and passed to the
     *             exception handler for this componenet
     */
    protected UMOMessage aggregateEvents(EventGroup events) throws RoutingException
    {
        try
        {
            return BankQuotesAggregationLogic.aggregateEvents(events);
        }
        catch (Exception e)
        {
            throw new AggregationException(events, null, e);
        }
    }
}
