/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.store;

import org.mule.api.MuleContext;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectStoreException;
import org.mule.util.queue.QueueKey;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PartitionedObjectStoreWrapper<T extends Serializable> implements ListableObjectStore<T>
{
    String partitionName;
    MuleContext context;
    ListableObjectStore<T> baseStore;

    public PartitionedObjectStoreWrapper(String name, MuleContext context, ListableObjectStore<T> store)
    {
        partitionName = name;
        this.context = context;
        baseStore = store;
    }

    @Override
    public boolean contains(Serializable key) throws ObjectStoreException
    {
        return getStore().contains(new QueueKey(partitionName, key));
    }

    @Override
    public void store(Serializable key, T value) throws ObjectStoreException
    {
        // This required because QueuePersistenceObject store will NOT complain in
        // cases where object already exists!
        QueueKey qKey = new QueueKey(partitionName, key);
        synchronized (this)
        {
            if (getStore().contains(qKey))
            {
                throw new ObjectAlreadyExistsException();
            }
            getStore().store(qKey, value);
        }
    }

    @Override
    public T retrieve(Serializable key) throws ObjectStoreException
    {
        return getStore().retrieve(new QueueKey(partitionName, key));
    }

    @Override
    public T remove(Serializable key) throws ObjectStoreException
    {
        return getStore().remove(new QueueKey(partitionName, key));
    }

    @Override
    public boolean isPersistent()
    {
        return getStore().isPersistent();
    }

    @Override
    public void open() throws ObjectStoreException
    {
        getStore().open();
    }

    @Override
    public void close() throws ObjectStoreException
    {
        getStore().close();
    }

    @Override
    public List<Serializable> allKeys() throws ObjectStoreException
    {
        // TODO this is NOT efficient!
        List<Serializable> results = new ArrayList<Serializable>();
        List<Serializable> keys = getStore().allKeys();
        for (Serializable key : keys)
        {
            QueueKey qKey = (QueueKey) key;
            if (qKey.queueName.equals(partitionName))
            {
                results.add(qKey.id);
            }
        }
        return results;
    }

    private ListableObjectStore<T> getStore()
    {
        return baseStore;
    }

    public ListableObjectStore<T> getBaseStore()
    {
        return getStore();
    }

}
