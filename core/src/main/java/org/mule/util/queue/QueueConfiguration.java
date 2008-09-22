/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
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

    // @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + capacity;
        result = prime * result + (persistent ? 1231 : 1237);
        return result;
    }

    // @Override
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
        QueueConfiguration other = (QueueConfiguration) obj;
        if (capacity != other.capacity)
        {
            return false;
        }
        if (persistent != other.persistent)
        {
            return false;
        }
        return true;
    }
    
}
