/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.routing.AggregationException;
import org.mule.routing.CollectionCorrelatorCallback;
import org.mule.routing.EventCorrelatorCallback;
import org.mule.routing.inbound.EventGroup;

/**
 * <code>AbstractCorrelationAggregatingMessageProcessor</code> uses the CorrelationID and
 * CorrelationGroupSize properties of the {@link org.mule.api.MuleMessage} to manage
 * message groups.
 */
public abstract class AbstractCorrelationAggregatingMessageProcessor extends AbstractEventAggregatingMessageProcessor
{

    @Override
    protected EventCorrelatorCallback getCorrelatorCallback(MuleEvent event)
    {
        return new DelegateCorrelatorCallback(event.getMuleContext());
    }

    protected abstract MuleMessage aggregateEvents(EventGroup events) throws AggregationException;

    private class DelegateCorrelatorCallback extends CollectionCorrelatorCallback
    {
        public DelegateCorrelatorCallback(MuleContext muleContext)
        {
            super(muleContext);
        }

        @Override
        public MuleMessage aggregateEvents(EventGroup events) throws AggregationException
        {
            return AbstractCorrelationAggregatingMessageProcessor.this.aggregateEvents(events);
        }
    }

}