/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
