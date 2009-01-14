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

import org.mule.DefaultMessageCollection;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessageCollection;
import org.mule.util.ClassUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.helpers.Utils;
import org.apache.commons.collections.IteratorUtils;

/**
 * <code>EventGroup</code> is a holder over events grouped by a common group Id.
 * This can be used by components such as routers to managed related events.
 */
// @ThreadSafe
public class EventGroup implements Comparable, Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 953739659615692697L;

    public static final MuleEvent[] EMPTY_EVENTS_ARRAY = new MuleEvent[0];

    private final Object groupId;
    // @GuardedBy("this")
    private final List events;
    private final long created;
    private final int expectedSize;

    public EventGroup(Object groupId)
    {
        this(groupId, -1);
    }

    public EventGroup(Object groupId, int expectedSize)
    {
        super();
        this.created = Utils.nanoTime();
        this.events = new ArrayList(expectedSize > 0 ? expectedSize : 10);
        this.expectedSize = expectedSize;
        this.groupId = groupId;
    }

    /**
     * Compare this EventGroup to another one. If the receiver and the argument both
     * have groupIds that are {@link Comparable}, they are used for the comparison;
     * otherwise - since the id can be any object - the group creation time stamp is
     * used as fallback. Older groups are considered "smaller".
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o)
    {
        EventGroup other = (EventGroup) o;
        Object otherId = other.getGroupId();

        if (groupId instanceof Comparable && otherId instanceof Comparable)
        {
            return ((Comparable) groupId).compareTo(otherId);
        }
        else
        {
            long diff = created - other.getCreated();
            return (diff > 0 ? 1 : (diff < 0 ? -1 : 0));
        }
    }

    /**
     * Compares two EventGroups for equality. EventGroups are considered equal when
     * their groupIds (as returned by {@link #getGroupId()}) are equal.
     * 
     * @see java.lang.Object#equals(Object)
     */
    // @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof EventGroup))
        {
            return false;
        }

        final EventGroup other = (EventGroup) obj;
        if (groupId == null)
        {
            return (other.groupId == null);
        }

        return groupId.equals(other.groupId);
    }

    /**
     * The hashCode of an EventGroup is derived from the object returned by
     * {@link #getGroupId()}.
     * 
     * @see java.lang.Object#hashCode()
     */
    // @Override
    public int hashCode()
    {
        return groupId.hashCode();
    }

    /**
     * Returns an identifier for this EventGroup. It is recommended that this id is
     * unique and {@link Comparable} e.g. a UUID.
     * 
     * @return the id of this event group
     */
    public Object getGroupId()
    {
        return groupId;
    }

    /**
     * Returns an iterator over a snapshot copy of this group's collected events. If
     * you need to iterate over the group and e.g. remove select events, do so via
     * {@link #removeEvent(MuleEvent)}. If you need to do so atomically in order to
     * prevent e.g. concurrent reception/aggregation of the group during iteration,
     * wrap the iteration in a synchronized block on the group instance.
     * 
     * @return an iterator over collected {@link MuleEvent}s.
     */
    public Iterator iterator()
    {
        synchronized (this)
        {
            if (events.isEmpty())
            {
                return IteratorUtils.emptyIterator();
            }
            else
            {
                return IteratorUtils.arrayIterator(this.toArray());
            }
        }
    }

    /**
     * Returns a snapshot of collected events in this group.
     * 
     * @return an array of collected {@link MuleEvent}s.
     */
    public MuleEvent[] toArray()
    {
        synchronized (this)
        {
            if (events.isEmpty())
            {
                return EMPTY_EVENTS_ARRAY;
            }

            return (MuleEvent[]) events.toArray(EMPTY_EVENTS_ARRAY);
        }
    }

    /**
     * Add the given event to this group.
     * 
     * @param event the event to add
     */
    public void addEvent(MuleEvent event)
    {
        synchronized (this)
        {
            events.add(event);
        }
    }

    /**
     * Remove the given event from the group.
     * 
     * @param event the evnt to remove
     */
    public void removeEvent(MuleEvent event)
    {
        synchronized (this)
        {
            events.remove(event);
        }
    }

    /**
     * Return the creation timestamp of the current group in nanoseconds.
     * 
     * @return the timestamp when this group was instantiated.
     * @see Utils#nanoTime()
     */
    public long getCreated()
    {
        return created;
    }

    /**
     * Returns the number of events collected so far.
     * 
     * @return number of events in this group or 0 if the group is empty.
     */
    public int size()
    {
        synchronized (this)
        {
            return events.size();
        }
    }

    /**
     * Returns the number of events that this EventGroup is expecting before
     * correlation can proceed.
     * 
     * @return expected number of events or -1 if no expected size was specified.
     */
    public int expectedSize()
    {
        return expectedSize;
    }

    /**
     * Removes all events from this group.
     */
    public void clear()
    {
        synchronized (this)
        {
            events.clear();
        }
    }

    // @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer(80);
        buf.append(ClassUtils.getSimpleName(this.getClass()));
        buf.append(" {");
        buf.append("id=").append(groupId);
        buf.append(", expected size=").append(expectedSize);

        synchronized (this)
        {
            int currentSize = events.size();
            buf.append(", current events=").append(currentSize);

            if (currentSize > 0)
            {
                buf.append(" [");
                Iterator i = events.iterator();
                while (i.hasNext())
                {
                    MuleEvent event = (MuleEvent) i.next();
                    buf.append(event.getMessage().getUniqueId());
                    if (i.hasNext())
                    {
                        buf.append(", ");
                    }
                }
                buf.append(']');
            }
        }

        buf.append('}');

        return buf.toString();
    }

    public MuleMessageCollection toMessageCollection()
    {
        MuleMessageCollection col = new DefaultMessageCollection();
        for (Iterator iterator = events.iterator(); iterator.hasNext();)
        {
            MuleEvent event = (MuleEvent) iterator.next();
            col.addMessage(event.getMessage());
        }
        return col;
    }

}
