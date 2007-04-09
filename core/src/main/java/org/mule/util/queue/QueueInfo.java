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

import java.util.LinkedList;

/**
 * Stores information about a Queue
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class QueueInfo
{
    protected LinkedList list;
    protected String name;
    protected QueueConfiguration config;

    public boolean equals(Object obj)
    {
        return (obj instanceof QueueInfo && name.equals(((QueueInfo) obj).name));
    }

    public int hashCode()
    {
        return name.hashCode();
    }

    public void putNow(Object o)
    {
        synchronized (list)
        {
            list.addLast(o);
            list.notifyAll();
        }
    }

    public boolean offer(Object o, int room, long timeout) throws InterruptedException
    {
        if (Thread.interrupted())
        {
            throw new InterruptedException();
        }
        synchronized (list)
        {
            if (config.capacity > 0)
            {
                if (config.capacity <= room)
                {
                    throw new IllegalStateException("Can not add more objects than the capacity in one time");
                }
                long l1 = timeout > 0L ? System.currentTimeMillis() : 0L;
                long l2 = timeout;
                while (list.size() >= config.capacity - room)
                {
                    if (l2 <= 0L)
                    {
                        return false;
                    }
                    list.wait(l2);
                    l2 = timeout - (System.currentTimeMillis() - l1);
                }
            }
            if (o != null)
            {
                list.addLast(o);
            }
            list.notifyAll();
            return true;
        }
    }

    public Object poll(long timeout) throws InterruptedException
    {
        if (Thread.interrupted())
        {
            throw new InterruptedException();
        }
        synchronized (list)
        {
            long l1 = timeout > 0L ? System.currentTimeMillis() : 0L;
            long l2 = timeout;
            while (list.isEmpty())
            {
                if (l2 <= 0L)
                {
                    return null;
                }
                list.wait(l2);
                l2 = timeout - (System.currentTimeMillis() - l1);
            }
            Object o = list.removeFirst();
            list.notifyAll();
            return o;
        }
    }

    public Object peek() throws InterruptedException
    {
        if (Thread.interrupted())
        {
            throw new InterruptedException();
        }
        synchronized (list)
        {
            if (list.isEmpty())
            {
                return null;
            }
            else
            {
                return list.getFirst();
            }
        }
    }

}
