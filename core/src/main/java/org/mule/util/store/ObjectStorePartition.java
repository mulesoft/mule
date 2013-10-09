/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.store;

import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.PartitionableObjectStore;

import java.io.Serializable;
import java.util.List;

public class ObjectStorePartition<T extends Serializable> implements ListableObjectStore<T>
{

    final String partitionName;
    final PartitionableObjectStore<T> partitionedObjectStore;

    public ObjectStorePartition(String partitionName, PartitionableObjectStore<T> partitionedObjectStore)
    {
        this.partitionName = partitionName;
        this.partitionedObjectStore = partitionedObjectStore;
    }

    @Override
    public boolean contains(Serializable key) throws ObjectStoreException
    {
        return partitionedObjectStore.contains(key, partitionName);
    }

    @Override
    public void store(Serializable key, T value) throws ObjectStoreException
    {
        partitionedObjectStore.store(key, value, partitionName);
    }

    @Override
    public T retrieve(Serializable key) throws ObjectStoreException
    {
        return partitionedObjectStore.retrieve(key, partitionName);
    }

    @Override
    public T remove(Serializable key) throws ObjectStoreException
    {
        return partitionedObjectStore.remove(key, partitionName);
    }

    @Override
    public boolean isPersistent()
    {
        return partitionedObjectStore.isPersistent();
    }

    @Override
    public void open() throws ObjectStoreException
    {
        partitionedObjectStore.open(partitionName);
    }

    @Override
    public void close() throws ObjectStoreException
    {
        partitionedObjectStore.close(partitionName);
    }

    @Override
    public List<Serializable> allKeys() throws ObjectStoreException
    {
        return partitionedObjectStore.allKeys(partitionName);
    }

    public PartitionableObjectStore<T> getBaseStore()
    {
        return partitionedObjectStore;
    }
    
    public String getPartitionName()
    {
        return partitionName;
    }
    
}
