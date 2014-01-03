/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.routing;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEvent;

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

/**
 * Immutable object used to provide all the necessary information to perform an
 * aggregation operation in one single parameter, helping to maintain consistent and
 * simple signatures across aggregators
 * 
 * @since 3.5.0
 */
public class AggregationContext
{

    private static final Predicate failedEventsPredicate = new Predicate()
    {

        @Override
        public boolean evaluate(Object object)
        {
            MuleEvent event = (MuleEvent) object;
            ExceptionPayload ep = event.getMessage().getExceptionPayload();
            return ep != null && ep.getException() != null;
        }
    };

    /**
     * the original event from wich the events to be aggregated were splitted from
     */
    private final MuleEvent originalEvent;

    /**
     * The events to be aggregated
     */
    private final List<MuleEvent> events;

    /**
     * Creates a new instance
     * 
     * @param originalEvent a {@link MuleEvent}. Cannot be <code>null</code>
     * @param events a {@link List} of {@link MuleEvent}. Cannot be <code>null</code>
     *            but could be empty. In that case, is up to each consumer to decide
     *            wether to fail or not
     */
    public AggregationContext(MuleEvent originalEvent, List<MuleEvent> events)
    {
        if (events == null)
        {
            throw new IllegalArgumentException("events cannot be null");
        }

        this.originalEvent = originalEvent;
        this.events = Collections.unmodifiableList(events);
    }

    /**
     * Returns all the {@link MuleEvent}s which messages have a not <code>null</code>
     * {@link ExceptionPayload} with a not <code>null</code>
     * {@link ExceptionPayload#getException()}. Notice that this is a select
     * operation. Each time this method is invoked the result will be re-calculated
     * 
     * @return a list of {@link MuleEvent}. It could be empty but it will never be
     *         <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public List<MuleEvent> selectEventsWithExceptions()
    {
        if (CollectionUtils.isEmpty(this.events))
        {
            Collections.emptyList();
        }

        return (List<MuleEvent>) CollectionUtils.select(this.events, failedEventsPredicate);
    }

    /**
     * The exact opposite to {@link #selectEventsWithExceptions()} Returns all the
     * {@link MuleEvent}s which messages have a <code>null</code>
     * {@link ExceptionPayload} or with a <code>null</code>
     * {@link ExceptionPayload#getException()}. Notice that this is a select
     * operation. Each time this method is invoked the result will be re-calculated
     * 
     * @return a list of {@link MuleEvent}. It could be empty but it will never be
     *         <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public List<MuleEvent> selectEventsWithoutExceptions()
    {
        if (CollectionUtils.isEmpty(this.events))
        {
            Collections.emptyList();
        }

        return (List<MuleEvent>) CollectionUtils.selectRejected(this.events, failedEventsPredicate);
    }

    public MuleEvent getOriginalEvent()
    {
        return this.originalEvent;
    }

    public List<MuleEvent> getEvents()
    {
        return this.events;
    }

}
