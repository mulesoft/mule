/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.correlation;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.store.ObjectStoreException;
import org.mule.routing.AggregationException;
import org.mule.routing.EventGroup;
import org.mule.routing.Resequencer;

import java.util.Arrays;
import java.util.Comparator;

/**
 * A Correlator that correlates messages based on Mule correlation settings. Note
 * that the {@link #aggregateEvents(org.mule.routing.EventGroup)} method only
 * resequences the events and returns an MuleEvent[] wrapped in a MuleMessage impl.
 * This means that this callback can ONLY be used with a {@link Resequencer}
 */
public class ResequenceCorrelatorCallback extends CollectionCorrelatorCallback
{
    protected Comparator<MuleEvent> eventComparator;

    public ResequenceCorrelatorCallback(Comparator<MuleEvent> eventComparator,
                                        MuleContext muleContext,
                                        boolean persistentStores,
                                        String storePrefix)
    {
        super(muleContext, persistentStores, storePrefix);
        this.eventComparator = eventComparator;
        this.muleContext = muleContext;
    }

    /**
     * This method is invoked if the shouldAggregate method is called and returns
     * true. Once this method returns an aggregated message, the event group is
     * removed from the router.
     * 
     * @param events the event group for this request
     * @return an aggregated message
     * @throws AggregationException if the aggregation fails. in this scenario the
     *             whole event group is removed and passed to the exception handler
     *             for this componenet
     */
    @Override
    public MuleEvent aggregateEvents(EventGroup events) throws AggregationException
    {
        MuleEvent results[];
        if (events == null || events.size() == 0)
        {
            results = EventGroup.EMPTY_EVENTS_ARRAY;
        }
        else
        {
            try
            {
                results = events.toArray(false);
            }
            catch (ObjectStoreException e)
            {
                throw new AggregationException(events, null, e);
            }
            Arrays.sort(results, eventComparator);
        }
        // This is a bit of a hack since we wrap the the collection of events in a
        // Mule Message to pass back
        return new DefaultMuleEvent(new DefaultMuleMessage(results, muleContext), results[0]);
    }

}
