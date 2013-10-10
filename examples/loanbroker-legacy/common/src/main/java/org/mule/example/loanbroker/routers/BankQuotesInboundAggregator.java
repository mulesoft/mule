/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.routers;

import org.mule.api.MuleEvent;
import org.mule.routing.AbstractCorrelationAggregator;
import org.mule.routing.AggregationException;
import org.mule.routing.EventGroup;

/**
 * <code>BankQuotesInboundAggregator</code> receives a number of quotes and selects the
 * lowest
 */
public class BankQuotesInboundAggregator extends AbstractCorrelationAggregator
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
    @Override
    protected MuleEvent aggregateEvents(EventGroup events) throws AggregationException
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
