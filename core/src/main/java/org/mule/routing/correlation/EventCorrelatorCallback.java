/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.correlation;

import org.mule.api.MuleEvent;
import org.mule.api.routing.RoutingException;
import org.mule.routing.AggregationException;
import org.mule.routing.EventGroup;

/**
 * A callback used to allow pluggable behaviour when correlating events
 */
public interface EventCorrelatorCallback
{
    /**
     * Determines if the event group is ready to be aggregated. if the group is ready
     * to be aggregated (this is entirely up to the application. it could be
     * determined by volume, last modified time or some oher criteria based on the
     * last event received).
     *
     * @param events The current event group received by the correlator
     * @return true if the group is ready for aggregation
     */
    public boolean shouldAggregateEvents(EventGroup events);

    /**
     * This method is invoked if the shouldAggregate method is called and returns
     * true. Once this method returns an aggregated message, the event group is
     * removed from the router.
     *
     * @param events the event group for this request
     * @return an aggregated message
     * @throws AggregationException if the aggregation fails. in this scenario the
     *                              whole event group is removed and passed to the exception handler
     *                              for this component
     */
    public MuleEvent aggregateEvents(EventGroup events) throws RoutingException;


    /**
     * Creates the event group with a specific correlation size based on the Mule
     * Correlation support
     *
     * @param id    The group id
     * @param event the current event
     * @return a new event group of a fixed size
     */
    public EventGroup createEventGroup(MuleEvent event, Object id);
}
