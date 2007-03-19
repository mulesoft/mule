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
import org.mule.routing.inbound.CorrelationAggregator;
import org.mule.routing.inbound.EventGroup;
import org.mule.umo.UMOMessage;

/**
 * <code>BankQuotesInboundAggregator</code> receives a number of quotes and selects the
 * lowest
 */
public class BankQuotesInboundAggregator extends CorrelationAggregator
{
    /**
     * This method is invoked if the shouldAggregate method is called and returns
     * true. Once this method returns an aggregated message the event group is
     * removed from the router
     * 
     * @param events the event group for this request
     * @return an aggregated message
     * @throws AggregationException if the aggregation fails. in this scenario the
     *             whole event group is removed and passed to the exception handler
     *             for this componenet
     */
    protected UMOMessage aggregateEvents(EventGroup events) throws AggregationException
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

    /**
     * Determines if the event group is ready to be aggregated; this is entirely up
     * to the application. It could be determined by volume, last modified time or
     * some other criteria based on the last event received.
     * 
     * @param events event group to examine
     * @return true if the events are ready to be aggregated
     */
    protected boolean shouldAggregateEvents(EventGroup events)
    {
        return super.shouldAggregateEvents(events);
    }

}
