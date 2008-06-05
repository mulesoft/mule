/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.loanbroker.routers;

import org.mule.api.MuleMessage;
import org.mule.routing.AggregationException;
import org.mule.routing.CollectionCorrelatorCallback;
import org.mule.routing.EventCorrelatorCallback;
import org.mule.routing.inbound.EventGroup;
import org.mule.routing.response.ResponseCorrelationAggregator;

/**
 * <code>BankQuotesInboundAggregator</code> receives a number of quotes and selects the
 * lowest
 */
public class BankQuotesResponseAggregator extends ResponseCorrelationAggregator
{
    //@Override
    protected EventCorrelatorCallback getCorrelatorCallback()
    {
        return new CollectionCorrelatorCallback()
        {

            public MuleMessage aggregateEvents(EventGroup events) throws AggregationException
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
