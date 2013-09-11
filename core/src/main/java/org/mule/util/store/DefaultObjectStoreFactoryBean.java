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
    /**
     * @deprecated to change object store implementations use the registry.
     * You must replace the object under keys:
     * - {@link org.mule.api.config.MuleProperties#OBJECT_STORE_DEFAULT_IN_MEMORY_NAME}
     * - {@link org.mule.api.config.MuleProperties#OBJECT_STORE_DEFAULT_PERSISTENT_NAME}
     * - {@link org.mule.api.config.MuleProperties#DEFAULT_USER_OBJECT_STORE_NAME}
     * - {@link org.mule.api.config.MuleProperties#DEFAULT_USER_TRANSIENT_OBJECT_STORE_NAME}
     * - {@link org.mule.api.config.MuleProperties#QUEUE_STORE_DEFAULT_IN_MEMORY_NAME}
     * - {@link org.mule.api.config.MuleProperties#QUEUE_STORE_DEFAULT_PERSISTENT_NAME}
     * @param factory
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
        if ("true".equals(System.getProperty("mule.objectstore.user.transient")))
        {
            return delegate.createDefaultUserTransientObjectStore();
        }
        return delegate.createDefaultUserObjectStore();
    }

    public static ObjectStore<Serializable> createDefaultUserTransientObjectStore()
    {
        return delegate.createDefaultUserTransientObjectStore();
    }
}
