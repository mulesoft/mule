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

/**
 * Manage the creation of the default Mule object stores.
 */
public class DefaultObjectStoreFactoryBean
{

    private static DefaultObjectStoreFactory delegate = new MuleDefaultObjectStoreFactory();

    /**
     * Do not instantiate
     */
    private DefaultObjectStoreFactoryBean()
    {
    }

    /**
     * Set a delegate to create the object stores in a non-default way
     */
    public static void setDelegate(DefaultObjectStoreFactory factory)
    {
        if (delegate == null)
        {
            throw new IllegalArgumentException("Object store factory cannot be null");
        }

        delegate = factory;
    }

    public static ObjectStore<Serializable> createDefaultInMemoryObjectStore()
    {
        return delegate.createDefaultInMemoryObjectStore();
    }

    public static ObjectStore<Serializable> createDefaultPersistentObjectStore()
    {
        return delegate.createDefaultPersistentObjectStore();
    }

    public static QueueStore<Serializable> createDefaultInMemoryQueueStore()
    {
        return delegate.createDefaultInMemoryQueueStore();
    }

    public static QueueStore<Serializable> createDefaultPersistentQueueStore()
    {
        return delegate.createDefaultPersistentQueueStore();
    }

    public static ObjectStore<Serializable> createDefaultUserObjectStore()
    {
        return delegate.createDefaultUserObjectStore();
    }
}
