/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.routing.correlation.CollectionCorrelatorCallback;
import org.mule.routing.correlation.EventCorrelatorCallback;

/**
 * <code>AbstractCorrelationAggregatingMessageProcessor</code> uses the CorrelationID
 * and CorrelationGroupSize properties of the {@link org.mule.api.MuleMessage} to
 * manage message groups.
 */
public abstract class AbstractCorrelationAggregator extends AbstractAggregator
{

    @Override
    protected EventCorrelatorCallback getCorrelatorCallback(MuleContext muleContext)
    {
        return new DelegateCorrelatorCallback(muleContext);
    }

    protected abstract MuleEvent aggregateEvents(EventGroup events) throws AggregationException;

    private class DelegateCorrelatorCallback extends CollectionCorrelatorCallback
    {
        public DelegateCorrelatorCallback(MuleContext muleContext)
        {
            super(muleContext, persistentStores, storePrefix);
        }

        @Override
        public MuleEvent aggregateEvents(EventGroup events) throws AggregationException
        {
            return AbstractCorrelationAggregator.this.aggregateEvents(events);
        }
    }

}
