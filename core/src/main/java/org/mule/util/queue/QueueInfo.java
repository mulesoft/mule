/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.queue;

import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores information about a Queue
 */
public class QueueInfo
{
    private QueueConfiguration config;
    private String name;
    private QueueInfoDelegate delegate;
    private static Map<Class<? extends ObjectStore>, QueueInfoDelegateFactory> delegateFactories = new HashMap<Class<? extends ObjectStore>, QueueInfoDelegateFactory>();

    public QueueInfo(String name, QueueConfiguration config)
    {
        this.name = name;
        setConfigAndDelegate(config);
    }

    public void setConfig(QueueConfiguration config)
    {
        setConfigAndDelegate(config);
    }

    private void setConfigAndDelegate(QueueConfiguration config)
    {
        boolean hadConfig = this.config != null;
        this.config = config;
        int capacity = 0;
        QueueInfoDelegateFactory factory = null;
        if (config != null)
        {
            capacity = config.capacity;
            factory = delegateFactories.get(TransactionalQueueManager.getActualStore(config.objectStore).getClass());
        }
        if (delegate == null || (config != null && !hadConfig))
        {
            this.delegate = factory != null ? factory.createDelegate(this) : new DefaultQueueInfoDelegate(capacity);
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        return (obj instanceof QueueInfo && name.equals(((QueueInfo) obj).name));
    }

    public String getName()
    {
        return name;
    }


    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    public void putNow(Serializable o)
    {
        delegate.putNow(o);
    }

    public boolean offer(Serializable o, int room, long timeout)
        throws InterruptedException
    {
        return delegate.offer(o, room, timeout);
    }

    public Serializable poll(long timeout)
        throws InterruptedException
    {
        return delegate.poll(timeout);
    }

    public Serializable peek()
        throws InterruptedException
    {
        return delegate.peek();
    }

    public void untake(Serializable item)
        throws InterruptedException
    {
        delegate.untake(item);
    }

    public int getSize()
    {
        return delegate.getSize();
    }

    public ListableObjectStore<Serializable> getStore()
    {
        return config == null ? null : TransactionalQueueManager.getActualStore(config.objectStore);
    }

    public static synchronized void registerDelegateFactory(Class<? extends ObjectStore>storeType, QueueInfoDelegateFactory factory)
    {
        delegateFactories.put(storeType, factory);
    }

    public int getCapacity()
    {
        return config == null ? null : config.capacity;
    }

    /**
     * A factory for creating object store-specific queue info delegates
     */
    public static interface QueueInfoDelegateFactory
    {
        /**
         * Create a delegate
         */
        QueueInfoDelegate createDelegate(QueueInfo parent);
    }
}
