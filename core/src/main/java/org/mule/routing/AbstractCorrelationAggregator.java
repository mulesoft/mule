/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
