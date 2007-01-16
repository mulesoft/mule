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
import org.mule.util.ClassUtils;

import edu.emory.mathcs.backport.java.util.concurrent.helpers.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    public static final UMOEvent[] EMPTY_EVENTS_ARRAY = new UMOEvent[0];

    private final Object groupId;
    // @GuardedBy("itself")
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

    // @Override
    // TODO HH: document
    public int compareTo(Object o)
    {
        EventGroup other = (EventGroup)o;
        Object otherId = other.getGroupId();

        if (groupId instanceof Comparable && otherId instanceof Comparable)
        {
            return ((Comparable)groupId).compareTo(otherId);
        }
        else
        {
            long diff = created - other.getCreated();
            return (diff > 0 ? 1 : (diff < 0 ? -1 : 0));
        }
    }

    // @Override
    // TODO HH: document
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (getClass() != obj.getClass())
        {
            return false;
        }

        final EventGroup other = (EventGroup)obj;
        if (groupId == null)
        {
            if (other.groupId != null)
            {
                return false;
            }
        }
        else
        {
            if (!groupId.equals(other.groupId))
            {
                return false;
            }
        }
        return true;
    }

    // @Override
    // TODO HH: document
    public int hashCode()
    {
        return groupId.hashCode();
    }

    public Object getGroupId()
    {
        return groupId;
    }

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

    public UMOEvent[] toArray()
    {
        synchronized (events)
        {
            if (events.isEmpty())
            {
                return EMPTY_EVENTS_ARRAY;
            }
            return (UMOEvent[])events.toArray(EMPTY_EVENTS_ARRAY);
        }
    }

    public void addEvent(UMOEvent event)
    {
        synchronized (events)
        {
            events.add(event);
        }
    }

    public void removeEvent(UMOEvent event)
    {
        synchronized (events)
        {
            events.remove(event);
        }
    }

    public long getCreated()
    {
        return created;
    }

    public int size()
    {
        synchronized (events)
        {
            return events.size();
        }
    }

    public void clear()
    {
        synchronized (events)
        {
            events.clear();
        }
    }

    public int expectedSize()
    {
        return expectedSize;
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer(80);
        buf.append(ClassUtils.getShortClassName(this.getClass()));
        buf.append(" {");
        buf.append("id=").append(groupId);
        buf.append(", expected size=").append(expectedSize);

        synchronized (events)
        {
            int currentSize = events.size();
            buf.append(", current events=").append(currentSize);

            if (currentSize > 0)
            {
                buf.append(" [");
                Iterator i = events.iterator();
                while (i.hasNext())
                {
                    UMOEvent event = (UMOEvent)i.next();
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

}
