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
import org.mule.umo.UMOMessage;
import org.mule.umo.routing.RoutingException;

/**
 * Handles single event responses from a replyTo address. If multiple responses will
 * be received for a single invocation, the {@link ResponseCorrelationAggregator}
 * should be used.
 */
public class SingleResponseRouter extends AbstractResponseAggregator
{
    /**
     * The <code>SingleResponseRouter</code> will return true if the event group
     * size is 1. If the group size is greater than 1, a warning will be logged.
     * 
     * @param events
     * @return true if the event group size is 1 or greater
     * @see {@link AbstractResponseAggregator#shouldAggregateEvents(EventGroup)}
     */
    protected boolean shouldAggregateEvents(EventGroup events)
    {
        if (events.expectedSize() > 1)
        {
            logger.warn("CorrelationGroup's expected size is not 1."
                            + " The SingleResponseAggregator will only handle single replyTo events;"
                            + " if there will be multiple events for a single request, "
                            + " use the 'ResponseCorrelationAggregator'");
        }

        return (events.size() != 0);
    }

    /**
     * The <code>SingleResponseRouter</code> will always return the first event of
     * an event group.
     * 
     * @param events the event group for this request
     * @return an aggregated message
     * @throws org.mule.umo.routing.RoutingException if the aggregation fails. in
     *             this scenario the whole event group is removed and passed to the
     *             exception handler for this componenet
     * @see {@link AbstractResponseAggregator#aggregateEvents(EventGroup)}
     */
    protected UMOMessage aggregateEvents(EventGroup events) throws RoutingException
    {
        return ((UMOEvent)events.iterator().next()).getMessage();
    }

}
