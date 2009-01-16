/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.inbound;

import org.mule.routing.CollectionCorrelatorCallback;
import org.mule.routing.EventCorrelatorCallback;

/**
 * This router will return all aggregated events as a {@link org.mule.api.MuleMessageCollection}.
 * This allows the service itself to act upon the events rather that the user having to write a custom
 * aggregator.  This may feel more natural for some users.
 */
public class SimpleCollectionAggregator extends AbstractEventAggregator
{
    protected EventCorrelatorCallback getCorrelatorCallback()
    {
        return new CollectionCorrelatorCallback();
    }
}
