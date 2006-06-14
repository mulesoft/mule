/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing.inbound;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;
import org.mule.umo.UMOEvent;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * <code>EventGroup</code> is a holder over events grouped by a common group
 * Id. This can be used by components such as routers to managed related events
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EventGroup implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -7337182983687406403L;

    private Object groupId;
    private List events;
    private long created;
    private int expectedSize = -1;

    public EventGroup(Object groupId, int expectedSize)
    {
        this(groupId);
        this.expectedSize = expectedSize;
    }

    public EventGroup(Object groupId)
    {
        this.groupId = groupId;
        events = new CopyOnWriteArrayList();
        created = System.currentTimeMillis();
    }

    public Object getGroupId()
    {
        return groupId;
    }

    public Iterator iterator()
    {
        return events.iterator();
    }

    public void addEvent(UMOEvent event)
    {
        events.add(event);
    }

    public void removeEvent(UMOEvent event)
    {
        events.remove(event);
    }

    public long getCreated()
    {
        return created;
    }

    public int size()
    {
        return events.size();
    }

    public int expectedSize()
    {
        return expectedSize;
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("Event Group Id=").append(groupId);
        buf.append(", expected size=").append(expectedSize);
        buf.append(", current events (").append(events.size()).append(")");
        for (Iterator iterator = events.iterator(); iterator.hasNext();) {
            UMOEvent event = (UMOEvent) iterator.next();
            buf.append(", ").append(event.getMessage().getUniqueId());
        }
        return buf.toString();
    }
}
