/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.inbound;

import org.mule.umo.UMOEvent;

/**
 * <code>CorrelationAggregator</code> uses the CorrelationID and
 * CorrelationGroupSize properties of the {@link org.mule.umo.UMOMessage} to manage
 * message groups.
 */
public abstract class CorrelationAggregator extends AbstractEventAggregator
{

    /**
     * TODO HH: writeme
     * 
     * @param event
     * @param groupId
     * @return
     */
    // @Override
    protected EventGroup createEventGroup(UMOEvent event, Object groupId)
    {
        return new EventGroup(groupId, event.getMessage().getCorrelationGroupSize());
    }

    /**
     * Determines if the event group is ready to be aggregated. if the group is ready
     * to be aggregated (this is entirely up to the application. it could be
     * determined by volume, last modified time or some ohter criteria based on the
     * last event received)
     * 
     * @param events
     * @return
     */
    protected boolean shouldAggregateEvents(EventGroup events)
    {
        int size = events.expectedSize();

        if (size == -1)
        {
            logger.warn("Correlation Group Size not set, but CorrelationAggregator is being used.  Message is being forwarded");
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
