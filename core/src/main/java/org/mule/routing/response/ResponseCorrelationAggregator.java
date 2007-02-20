/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.response;

import org.mule.routing.inbound.EventGroup;
import org.mule.umo.UMOEvent;

/**
 * <code>ResponseCorrelationAggregator</code> Correlates one or more events on a
 * response flow using the Correlation Id to group events.
 */

public abstract class ResponseCorrelationAggregator extends AbstractResponseAggregator
{
    /**
     * Determines if the event group is ready to be aggregated. if the group is ready
     * to be aggregated (this is entirely up to the application. it could be
     * determined by volume, last modified time or some oher criteria based on the
     * last event received)
     * 
     * @param events
     * @return true if the event group is ready of aggregation
     */
    protected boolean shouldAggregateEvents(EventGroup events)
    {
        int expected = events.expectedSize();
        int current = events.size();

        if (expected == -1)
        {
            logger.warn("Correlation Group Size not set, but CorrelationAggregator is being used.  Message is being forwarded");
            return true;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Correlation size is " + expected + ", current event group size is " + current
                         + " for correlation group " + events.getGroupId());
        }

        return expected == current;
    }

    /**
     * Creates the event group with a specific correlation size based on the Mule
     * Correlation support
     * 
     * @param id The group id
     * @param event the current event
     * @return a new event group of a fixed size
     */
    // //@Override
    protected EventGroup createEventGroup(UMOEvent event, Object id)
    {
        return new EventGroup(id, event.getMessage().getCorrelationGroupSize());
    }

}
