/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.correlation;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.routing.AggregationException;
import org.mule.routing.EventGroup;
import org.mule.session.DefaultMuleSession;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A Correlator that correlates messages based on Mule correlation settings
 */
public class CollectionCorrelatorCallback implements EventCorrelatorCallback
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(getClass());

    protected MuleContext muleContext;
    private final boolean persistantStores;
    private final String storePrefix;

    public CollectionCorrelatorCallback(MuleContext muleContext, boolean persistantStores, String storePrefix)
    {
        this.muleContext = muleContext;
        this.persistantStores = persistantStores;
        this.storePrefix = storePrefix;
    }

    /**
     * This method is invoked if the shouldAggregate method is called and returns true. Once this method
     * returns an aggregated message, the event group is removed from the router.
     * 
     * @param events the event group for this request
     * @return an aggregated message
     * @throws org.mule.routing.AggregationException if the aggregation fails. in this scenario the whole
     *             event group is removed and passed to the exception handler for this componenet
     */
    public MuleEvent aggregateEvents(EventGroup events) throws AggregationException
    {
        return events.getMessageCollectionEvent();
    }

    protected MuleSession getMergedSession(MuleEvent[] events)
    {
        MuleSession session = new DefaultMuleSession(events[0].getSession());
        for (int i = 1; i < events.length; i++)
        {
            for (String name : events[i].getSession().getPropertyNamesAsSet())
            {
                session.setProperty(name, events[i].getSession().getProperty(name));
            }
        }
        return session;
    }

    /**
     * Creates a new EventGroup that will expect the number of events as returned by
     * {@link MuleMessage#getCorrelationGroupSize()}.
     */
    public EventGroup createEventGroup(MuleEvent event, Object groupId)
    {
        return new EventGroup(groupId, muleContext, event.getMessage().getCorrelationGroupSize(),
            persistantStores, storePrefix);
    }

    /**
     * @return <code>true</code> if the correlation size is not set or exactly the expected size of the event
     *         group.
     * @see org.mule.routing.correlation.EventCorrelatorCallback#shouldAggregateEvents(org.mule.routing.EventGroup)
     */
    public boolean shouldAggregateEvents(EventGroup events)
    {
        int size = events.expectedSize();

        if (size == -1)
        {
            logger.warn("Correlation Group Size not set, but correlation aggregator is being used."
                        + " Message is being forwarded as is");
            return true;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(MessageFormat.format(
                "Correlation group size is {0}. Current event group size is {1} for group ID: {2}", size,
                events.size(), events.getGroupId()));
        }

        return size == events.size();
    }
}
