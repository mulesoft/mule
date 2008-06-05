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

import org.mule.api.MuleMessage;
import org.mule.routing.AggregationException;
import org.mule.routing.CollectionCorrelatorCallback;
import org.mule.routing.EventCorrelatorCallback;

/**
 * <code>CorrelationAggregator</code> uses the CorrelationID and
 * CorrelationGroupSize properties of the {@link org.mule.api.MuleMessage} to manage
 * message groups.
 */
public abstract class CorrelationAggregator extends AbstractEventAggregator
{

    protected EventCorrelatorCallback getCorrelatorCallback()
    {
        return new DelegateCorrelatorCallback();
    }

    protected abstract MuleMessage aggregateEvents(EventGroup events) throws AggregationException;

    private class DelegateCorrelatorCallback extends CollectionCorrelatorCallback
    {
        public MuleMessage aggregateEvents(EventGroup events) throws AggregationException
        {
            return CorrelationAggregator.this.aggregateEvents(events);
        }
    }

}
