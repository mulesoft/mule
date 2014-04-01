/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.QueueStore;

import java.io.Serializable;
import java.util.List;

/**
 * Adapts a {@link ListableObjectStore} to make it useful to store event queues.
 *
 * @deprecated this class will be removed in Mule 4.0 in favor of the new queue implementation
 */
@Deprecated
public class QueueStoreAdapter<T extends Serializable> implements QueueStore<T>, MuleContextAware
{

    private final ListableObjectStore<T> store;

    public QueueStoreAdapter(ListableObjectStore<T> store)
    {
        this.store = store;
    }

    @Override
    public void open() throws ObjectStoreException
    {
        store.open();
    }

    @Override
    public void close() throws ObjectStoreException
    {
        store.close();
    }

    @Override
    public List<Serializable> allKeys() throws ObjectStoreException
    {
        return store.allKeys();
    }

    @Override
    public boolean contains(Serializable key) throws ObjectStoreException
    {
        return store.contains(key);
    }

    @Override
    public void store(Serializable key, T value) throws ObjectStoreException
    {
        store.store(key, value);
    }

    @Override
    public T retrieve(Serializable key) throws ObjectStoreException
    {
        return store.retrieve(key);
    }

    @Override
    public T remove(Serializable key) throws ObjectStoreException
    {
        return store.remove(key);
    }
    
    @Override
    public void clear() throws ObjectStoreException
    {
        this.store.clear();
    }

    @Override
    public boolean isPersistent()
    {
        return store.isPersistent();
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        if (store instanceof MuleContextAware)
        {
            ((MuleContextAware) store).setMuleContext(context);
        }
    }
}
