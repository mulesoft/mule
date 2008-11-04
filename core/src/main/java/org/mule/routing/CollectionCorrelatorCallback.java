/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.DefaultMessageCollection;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.routing.inbound.EventGroup;

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
    protected transient final Log logger = LogFactory.getLog(CollectionCorrelatorCallback.class);

    /**
     * This method is invoked if the shouldAggregate method is called and returns
     * true. Once this method returns an aggregated message, the event group is
     * removed from the router.
     *
     * @param events the event group for this request
     * @return an aggregated message
     * @throws org.mule.routing.AggregationException
     *          if the aggregation fails. in this scenario the
     *          whole event group is removed and passed to the exception handler
     *          for this componenet
     */
    public MuleMessage aggregateEvents(EventGroup events) throws AggregationException
    {
        MuleMessageCollection message = new DefaultMessageCollection();
        message.addMessages(events.toArray());
        return message;
    }

    /**
     * Creates a new EventGroup that will expect the number of events as returned by
     * {@link org.mule.api.transport.MessageAdapter#getCorrelationGroupSize()}.
     */
    // @Override
    public EventGroup createEventGroup(MuleEvent event, Object groupId)
    {
        return new EventGroup(groupId, event.getMessage().getCorrelationGroupSize());
    }

    /**
     * @return <code>true</code> if the correlation size is not set or exactly the
     *         expected size of the event group.
     * @see org.mule.routing.inbound.AbstractEventAggregator#shouldAggregateEvents(EventGroup)
     */
    public boolean shouldAggregateEvents(EventGroup events)
    {
        int size = events.expectedSize();

        if (size == -1)
        {
            logger.warn("Correlation Group Size not set, but AbstractCorrelationAggregator is being used."
                    + " Message is being forwarded");
            return true;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Correlation size is " + size + ". Current event group size is " + events.size()
                    + " for correlation " + events.getGroupId());
        }

        return size == events.size();
    }

}
