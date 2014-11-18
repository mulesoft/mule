/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    public void clear() throws ObjectStoreException
    {
        this.partitionedObjectStore.clear(this.partitionName);
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
