/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.response;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.routing.RoutingException;
import org.mule.routing.EventCorrelatorCallback;
import org.mule.routing.inbound.EventGroup;

/**
 * <code>ResponseCorrelationAggregator</code> Correlates one or more events on a
 * response flow using the Correlation Id to group events.
 */

public abstract class ResponseCorrelationAggregator extends AbstractResponseAggregator
{

    protected EventCorrelatorCallback getCorrelatorCallback()
    {
        return new DefaultEventCorrelatorCallback();
    }

    public class DefaultEventCorrelatorCallback implements EventCorrelatorCallback
    {
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
         *          for this component
         */
        public MuleMessage aggregateEvents(EventGroup events) throws RoutingException
        {
            return ResponseCorrelationAggregator.this.aggregateEvents(events);
        }

        /**
         * Determines if the event group is ready to be aggregated. if the group is ready
         * to be aggregated (this is entirely up to the application. it could be
         * determined by volume, last modified time or some oher criteria based on the
         * last event received)
         *
         * @param events
         * @return true if the event group is ready of aggregation
         */
        public boolean shouldAggregateEvents(EventGroup events)
        {
            int expected = events.expectedSize();
            int current = events.size();

            if (expected == -1)
            {
                logger.warn("Correlation Group Size not set, but AbstractCorrelationAggregator is being used.  Message is being forwarded");
                return true;
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Correlation size is " + expected + ", current event group size is " + current
                        + " for correlation group " + events.getGroupId());
            }

            return expected == current;
        }

        public EventGroup createEventGroup(MuleEvent event, Object id)
        {
            return new EventGroup(id, event.getMessage().getCorrelationGroupSize());
    }
    }
}
