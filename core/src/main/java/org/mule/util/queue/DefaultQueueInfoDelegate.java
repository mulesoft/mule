/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.queue;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;

/**
 * The default QueueInfoDelegate. This uses a LinkedList to store the members of the queue.
 */
public class DefaultQueueInfoDelegate implements TransientQueueInfoDelegate
{
    protected final int capacity;
    protected final LinkedList<Serializable> list;

    public DefaultQueueInfoDelegate(int capacity)
    {
        this.capacity = capacity;
        list = new LinkedList<Serializable>();
    }

    @Override
    public void putNow(Serializable o)
    {
        synchronized (list)
        {
            list.addLast(o);
            list.notifyAll();
        }
    }

    @Override
    public boolean offer(Serializable o, int room, long timeout) throws InterruptedException
    {
        if (Thread.interrupted())
        {
            throw new InterruptedException();
        }
        synchronized (list)
        {
            if (capacity > 0)
            {
                if (capacity <= room)
                {
                    throw new IllegalStateException("Can not add more objects than the capacity in one time");
                }
                long l1 = timeout > 0L ? System.currentTimeMillis() : 0L;
                long l2 = timeout;
                while (list.size() >= capacity - room)
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

    @Override
    public Serializable poll(long timeout) throws InterruptedException
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

            Serializable o = list.removeFirst();
            list.notifyAll();
            return o;
        }
    }

    @Override
    public Serializable peek() throws InterruptedException
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

    @Override
    public void untake(Serializable item) throws InterruptedException
    {
        if (Thread.interrupted())
        {
            throw new InterruptedException();
        }
        synchronized (list)
        {
            list.addFirst(item);
        }
    }

    @Override
    public int getSize()
    {
        return list.size();
    }

    @Override
    public boolean addAll(Collection<? extends Serializable> items)
    {
        synchronized (list)
        {
            boolean result = list.addAll(items);
            list.notifyAll();
            return result;
        }
    }
}
