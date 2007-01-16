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

import org.mule.impl.MuleEvent;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.routing.AggregationException;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;

/**
 * <code>AbstractEventAggregator</code> will aggregate a set of messages into a
 * single message.
 */

public abstract class AbstractEventAggregator extends SelectiveConsumer
{
    public static final String NO_CORRELATION_ID = "no-id";

    private final ConcurrentMap eventGroups = new ConcurrentHashMap();

    // @Override
    public UMOEvent[] process(UMOEvent event) throws MessagingException
    {
        UMOEvent[] result = null;

        if (this.isMatch(event))
        {
            // indicates interleaved EventGroup removal (very rare)
            boolean miss = false;

            // match event to its group
            final Object groupId = this.getEventGroupIdForEvent(event);

            // spinloop for the EventGroup lookup
            while (true)
            {
                if (miss)
                {
                    try
                    {
                        // recommended over Thread.yield()
                        Thread.sleep(1);
                    }
                    catch (InterruptedException interrupted)
                    {
                        Thread.currentThread().interrupt();
                    }
                }

                // check for an existing group first
                EventGroup group = this.getEventGroupWithId(groupId);

                // does the group exist?
                if (group == null)
                {
                    // ..apparently not, so create a new one & add it
                    group = this.addEventGroup(this.createEventGroup(event, groupId));
                }

                // ensure that only one thread at a time evaluates this EventGroup
                synchronized (group)
                {
                    // make sure no other thread removed the group in the meantime
                    if (group != this.getEventGroupWithId(groupId))
                    {
                        // if that is the (rare) case, spin
                        miss = true;
                        continue;
                    }

                    // add the incoming event to the group
                    group.addEvent(event);

                    if (this.shouldAggregateEvents(group))
                    {
                        UMOMessage returnMessage = this.aggregateEvents(group);
                        UMOEndpoint endpoint = new MuleEndpoint(event.getEndpoint());
                        endpoint.setTransformer(null);
                        endpoint.setName(this.getClass().getName());
                        UMOEvent returnEvent = new MuleEvent(returnMessage, endpoint, event.getComponent(),
                            event);
                        result = new UMOEvent[]{returnEvent};
                        this.removeEventGroup(group);
                    }

                    // result or not: exit spinloop
                    break;
                }
            }
        }

        return result;
    }

    /**
     * TODO HH: writeme
     * 
     * @param event
     * @param groupId
     * @return
     */
    protected EventGroup createEventGroup(UMOEvent event, Object groupId)
    {
        return new EventGroup(groupId);
    }

    /**
     * TODO HH: writeme
     * 
     * @param event
     * @return
     */
    protected Object getEventGroupIdForEvent(UMOEvent event)
    {
        String groupId = event.getMessage().getCorrelationId();

        if (groupId == null)
        {
            groupId = NO_CORRELATION_ID;
        }

        return groupId;
    }

    /**
     * TODO HH: writeme
     * @param groupId
     * @return
     */
    protected EventGroup getEventGroupWithId(Object groupId)
    {
        return (EventGroup)eventGroups.get(groupId);
    }

    /**
     * TODO HH: writeme
     * 
     * @param group
     * @return
     */
    protected EventGroup addEventGroup(EventGroup group)
    {
        // a parallel thread might have removed the EventGroup already,
        // therefore we need to validate our current reference
        EventGroup previous = (EventGroup)eventGroups.putIfAbsent(group.getGroupId(), group);
        return (previous != null ? previous : group);        
    }

    /**
     * TODO HH: writeme
     * 
     * @param group
     */
    protected void removeEventGroup(EventGroup group)
    {
        eventGroups.remove(group.getGroupId());
    }

    /**
     * Determines if the event group is ready to be aggregated. if the group is ready
     * to be aggregated (this is entirely up to the application. it could be
     * determined by volume, last modified time or some oher criteria based on the
     * last event received).
     * 
     * @param events
     * @return true if the group is ready for aggregation
     */
    protected abstract boolean shouldAggregateEvents(EventGroup events);

    /**
     * This method is invoked if the shouldAggregate method is called and returns
     * true. Once this method returns an aggregated message, the event group is
     * removed from the router.
     * 
     * @param events the event group for this request
     * @return an aggregated message
     * @throws AggregationException if the aggregation fails. in this scenario the
     *             whole event group is removed and passed to the exception handler
     *             for this componenet
     */
    protected abstract UMOMessage aggregateEvents(EventGroup events) throws AggregationException;

}
