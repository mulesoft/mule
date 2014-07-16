/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.routers;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.routing.AbstractAggregator;
import org.mule.routing.AggregationException;
import org.mule.routing.EventGroup;
import org.mule.routing.correlation.CollectionCorrelatorCallback;
import org.mule.routing.correlation.EventCorrelatorCallback;

/**
 * <code>BankQuotesInboundAggregator</code> receives a number of quotes and selects the
 * lowest
 */
public class BankQuotesResponseAggregator extends AbstractAggregator
{
    @Override
    protected EventCorrelatorCallback getCorrelatorCallback(MuleContext context)
    {
        return new CollectionCorrelatorCallback(context,persistentStores,storePrefix)
        {
            @Override
            public MuleEvent aggregateEvents(EventGroup events) throws AggregationException
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
        };
    }
}
