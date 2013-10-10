/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.queue;

import org.mule.api.MuleContext;
import org.mule.api.store.QueueStore;

import java.io.Serializable;

public class QueueConfiguration
{
    protected final int capacity;
    protected final QueueStore<Serializable> objectStore;

    public QueueConfiguration(MuleContext context, int capacity, QueueStore<Serializable> objectStore)
    {
        this.capacity = capacity;
        this.objectStore = objectStore;
    }

    public QueueConfiguration(int capacity, QueueStore<Serializable> objectStore)
    {
        this.capacity = capacity;
        this.objectStore = objectStore;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + capacity;
        result = prime * result + objectStore.hashCode();
        return result;
    }

    @Override
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
        if (!objectStore.equals(objectStore))
        {
            return false;
        }
        return true;
    }

    public boolean isPersistent()
    {
        return objectStore.isPersistent();
    }
}
