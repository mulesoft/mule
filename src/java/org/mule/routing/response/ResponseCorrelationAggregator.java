/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing.response;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.routing.inbound.EventGroup;
import org.mule.umo.UMOEvent;
import org.mule.umo.routing.RoutingException;

/**
 * <code>ResponseCorrelationAggregator</code> Correlates one or more events on a response
 * flow using the Correlation Id to group events
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public abstract class ResponseCorrelationAggregator extends AbstractResponseAggregator
{
   /**
     * Determines if the event group is ready to be aggregated.
     * if the group is ready to be aggregated (this is entirely up
     * to the application. it could be determined by volume, last modified time
     * or some oher criteria based on the last event received)
     *
     * @param events
     * @return
     */
    protected boolean shouldAggregate(EventGroup events)
    {
        int size = events.getExpectedSize();
        if (size == -1) {
            logger.warn("Correlation Group Size not set, but CorrelationAggregator is being used.  Message is being forwarded");
            return true;
        }

        logger.info("Aggregator: Current Event groups = " + eventGroups.size());
        logger.info("correlation size is " + size + ". current event group size is " + events.getSize() + " for correlation " + events.getGroupId());
        return size == events.getSize();
    }

    /**
     * Adds the event to an event group. Groups are defined by the correlationId
     * on the message.  If no correlationId is set a default group is created for
     * all events without a correlationId.
     * If there is no group for the current correlationId one will be created and added
     * to the router.
     *
     * @param event
     * @return
     */
    protected EventGroup addEvent(UMOEvent event) throws RoutingException
    {
        String cId = event.getMessage().getCorrelationId();
        int groupSize = event.getMessage().getCorrelationGroupSize();
        if (cId == null) {
            throw new RoutingException(new Message(Messages.NO_CORRELATION_ID), event.getMessage(), event.getEndpoint());
        }
        EventGroup eg = (EventGroup) eventGroups.get(cId);
        if (eg == null)
        {
            eg = new EventGroup(cId, groupSize);
            eg.addEvent(event);
            eventGroups.put(eg.getGroupId(), eg);
        } else
        {
            eg.addEvent(event);
        }
        return eg;
    }
}
