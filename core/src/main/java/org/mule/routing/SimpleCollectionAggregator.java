/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.MuleContext;
import org.mule.routing.correlation.CollectionCorrelatorCallback;
import org.mule.routing.correlation.EventCorrelatorCallback;

/**
 * This router will return all aggregated events as a {@link org.mule.api.MuleMessageCollection}.
 * This allows the service itself to act upon the events rather that the user having to write a custom
 * aggregator.  This may feel more natural for some users.
 *
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Aggregator.html">http://www.eaipatterns.com/Aggregator.html</a>
 */
public class SimpleCollectionAggregator extends AbstractAggregator
{
    @Override
    protected EventCorrelatorCallback getCorrelatorCallback(MuleContext muleContext)
    {
        return new CollectionCorrelatorCallback(muleContext);
    }
}
