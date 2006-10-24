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
 * <code>CorrelationAggregator</code> Uses the CorrelationID and
 * CorrelationGroupSize properties of the {@link org.mule.umo.UMOMessage} to manage
 * message groups.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class CorrelationAggregator extends AbstractEventAggregator
{

    /**
     * Determines if the event group is ready to be aggregated. if the group is ready
     * to be aggregated (this is entirely up to the application. it could be
     * determined by volume, last modified time or some oher criteria based on the
     * last event received)
     * 
     * @param events
     * @return
     */
    protected boolean shouldAggregate(EventGroup events)
    {
        int size = events.expectedSize();
        if (size == -1)
        {
            logger.warn("Correlation Group Size not set, but CorrelationAggregator is being used.  Message is being forwarded");
            return true;
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Aggregator: Current Event groups = " + eventGroups.size());
            logger.debug("correlation size is " + size + ". current event group size is " + events.size()
                         + " for correlation " + events.getGroupId());
        }
        return size == events.size();
    }

    /**
     * Adds the event to an event group. Groups are defined by the correlationId on
     * the message. If no correlationId is set a default group is created for all
     * events without a correlationId. If there is no group for the current
     * correlationId one will be created and added to the router.
     * 
     * @param event
     * @return
     */
    protected EventGroup addEvent(UMOEvent event)
    {
        String cId = event.getMessage().getCorrelationId();
        int groupSize = event.getMessage().getCorrelationGroupSize();
        if (cId == null)
        {
            cId = NO_CORRELATION_ID;
        }
        EventGroup eg = (EventGroup)eventGroups.get(cId);
        if (eg == null)
        {
            eg = new EventGroup(cId, groupSize);
            eg.addEvent(event);
            eventGroups.put(eg.getGroupId(), eg);
        }
        else
        {
            eg.addEvent(event);
        }
        return eg;
    }
}
