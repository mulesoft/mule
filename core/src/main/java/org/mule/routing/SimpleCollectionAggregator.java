/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing;

import org.mule.api.MuleContext;
import org.mule.routing.correlation.CollectionCorrelatorCallback;
import org.mule.routing.correlation.EventCorrelatorCallback;

/**
 * This router will return all aggregated events as a
 * {@link org.mule.api.MuleMessageCollection}. This allows the service itself to act
 * upon the events rather that the user having to write a custom aggregator. This may
 * feel more natural for some users. <b>EIP Reference:</b> <a
 * href="http://www.eaipatterns.com/Aggregator.html"
 * >http://www.eaipatterns.com/Aggregator.html</a>
 */
public class SimpleCollectionAggregator extends AbstractAggregator
{
    @Override
    protected EventCorrelatorCallback getCorrelatorCallback(MuleContext muleContext)
    {
        return new CollectionCorrelatorCallback(muleContext, persistentStores, storePrefix);
    }
}
