/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.inbound;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;

import java.util.Arrays;
import java.util.Comparator;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;

/**
 * <code>AbstractEventResequencer</code> is used to receive a set of events,
 * resequence them and forward them on to their destination
 */

// TODO MULE-841: much of the code here (like the spinloop) is *exactly* the same as in
// AbstractEventAggregator, obviously we should unify this
public abstract class AbstractEventResequencer extends SelectiveConsumer
{
    protected static final String NO_CORRELATION_ID = "no-id";

    private final ConcurrentMap eventGroups = new ConcurrentHashMap();
    private volatile Comparator comparator;

    public AbstractEventResequencer()
    {
        super();
    }

    public Comparator getComparator()
    {
        return comparator;
    }

    public void setComparator(Comparator eventComparator)
    {
        this.comparator = eventComparator;
    }

    // @Override
    public MuleEvent[] process(MuleEvent event) throws MessagingException
    {
        MuleEvent[] result = null;

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
                EventGroup group = this.getEventGroup(groupId);

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
                    if (group != this.getEventGroup(groupId))
                    {
                        // if that is the (rare) case, spin
                        miss = true;
                        continue;
                    }

                    // add the incoming event to the group
                    group.addEvent(event);

                    if (this.shouldResequenceEvents(group))
                    {
                        result = this.resequenceEvents(group);
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
     * @see AbstractEventAggregator#createEventGroup(MuleEvent, Object)
     */
    protected EventGroup createEventGroup(MuleEvent event, Object groupId)
    {
        return new EventGroup(groupId);
    }

    /**
     * @see AbstractEventAggregator#getEventGroupIdForEvent(MuleEvent)
     */
    protected Object getEventGroupIdForEvent(MuleEvent event)
    {
        String groupId = event.getMessage().getCorrelationId();

        if (groupId == null)
        {
            groupId = NO_CORRELATION_ID;
        }

        return groupId;
    }

    /**
     * @see AbstractEventAggregator#getEventGroup(Object)
     */
    protected EventGroup getEventGroup(Object groupId)
    {
        return (EventGroup) eventGroups.get(groupId);
    }

    /**
     * @see AbstractEventAggregator#addEventGroup(EventGroup)
     */
    protected EventGroup addEventGroup(EventGroup group)
    {
        EventGroup previous = (EventGroup) eventGroups.putIfAbsent(group.getGroupId(), group);
        // a parallel thread might have removed the EventGroup already,
        // therefore we need to validate our current reference
        return (previous != null ? previous : group);
    }

    /**
     * @see AbstractEventAggregator#removeEventGroup(EventGroup)
     */
    protected void removeEventGroup(EventGroup group)
    {
        eventGroups.remove(group.getGroupId());
    }

    /**
     * Reorder collected events according to the configured Comparator.
     * 
     * @param events the EventGroup used for collecting the events
     * @return an array of events reordered according to the Comparator returned by
     *         {@link #getComparator()}. If no comparator is configured, the events
     *         are returned unsorted.
     */
    protected MuleEvent[] resequenceEvents(EventGroup events)
    {
        if (events == null || events.size() == 0)
        {
            return EventGroup.EMPTY_EVENTS_ARRAY;
        }

        MuleEvent[] result = events.toArray();
        Comparator cmp = this.getComparator();

        if (cmp != null)
        {
            Arrays.sort(result, cmp);
        }
        else
        {
            logger.debug("MuleEvent comparator is null, events were not reordered");
        }

        return result;
    }

    /**
     * Determines whether the events in the passed EventGroup are ready to be
     * reordered.
     * 
     * @see AbstractEventAggregator#shouldAggregateEvents(EventGroup)
     */
    protected abstract boolean shouldResequenceEvents(EventGroup events);

}
