/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing.inbound;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;
import org.mule.umo.UMOEvent;

import java.io.Serializable;
import java.util.List;

/**
 * <code>EventGroup</code> is a holder over events grouped by a common group
 * Id.  This can be used by components such as routers to managed related events
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class EventGroup implements Serializable
{
    private String groupId;
    private List events;
    private long created;
    private int expectedSize = -1;

    public EventGroup(String groupId, int expectedSize)
    {
        this(groupId);
        this.expectedSize = expectedSize;
    }

    public EventGroup(String groupId)
    {
        this.groupId = groupId;
        events = new CopyOnWriteArrayList();
        created = System.currentTimeMillis();
    }

    public String getGroupId()
    {
        return groupId;
    }

    public List getEvents()
    {
        return events;
    }

    public void addEvent(UMOEvent event) {
        events.add(event);
    }

    public void removeEvent(UMOEvent event) {
        events.remove(event);
    }

    public long getCreated()
    {
        return created;
    }

    public int getSize() {
        return events.size();
    }

    public int getExpectedSize()
    {
        return expectedSize;
    }
}
