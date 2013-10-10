/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util.store;

import org.mule.api.store.ObjectStore;
import org.mule.api.store.QueueStore;

import java.io.Serializable;

public class MuleDefaultObjectStoreFactory implements DefaultObjectStoreFactory
{

    @Override
    public ObjectStore<Serializable> createDefaultInMemoryObjectStore()
    {
        return new SimpleMemoryObjectStore<Serializable>();
    }

    @Override
    public ObjectStore<Serializable> createDefaultPersistentObjectStore()
    {
        return new PartitionedPersistentObjectStore<Serializable>();
    }

    @Override
    public QueueStore<Serializable> createDefaultInMemoryQueueStore()
    {
        return new QueueStoreAdapter<Serializable>(new SimpleMemoryObjectStore<Serializable>());
    }

    @Override
    public QueueStore<Serializable> createDefaultPersistentQueueStore()
    {
        return new QueueStoreAdapter<Serializable>(new QueuePersistenceObjectStore<Serializable>());
    }

    @Override
    public ObjectStore<Serializable> createDefaultUserObjectStore()
    {
        return new SimpleMemoryObjectStore<Serializable>();
    }
}
