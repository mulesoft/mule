/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.queue;

public class QueueConfiguration
{

    protected int capacity;
    protected boolean persistent;

    public QueueConfiguration(int capacity, boolean persistent)
    {
        this.capacity = capacity;
        this.persistent = persistent;
    }

    public QueueConfiguration(int capacity)
    {
        this(capacity, false);
    }

    public QueueConfiguration(boolean persistent)
    {
        this(0, persistent);
    }

    public QueueConfiguration()
    {
        this(0, false);
    }

    /**
     * @return Returns the capacity.
     */
    public int getCapacity()
    {
        return capacity;
    }

    /**
     * @param capacity The capacity to set.
     */
    public void setCapacity(int capacity)
    {
        this.capacity = capacity;
    }

    /**
     * @return Returns the persistent.
     */
    public boolean isPersistent()
    {
        return persistent;
    }

    /**
     * @param persistent The persistent to set.
     */
    public void setPersistent(boolean persistent)
    {
        this.persistent = persistent;
    }

}
