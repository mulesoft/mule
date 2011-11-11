/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.api.store.ObjectStore;

import java.io.Serializable;

/**
 * Manage the creation of the default Mule object stores.
 */
public class DefaultObjectStoreFactoryBean
{
    private static DefaultObjectStoreFactory delegate;

    /**
     * Do not instantiate
     */
    private DefaultObjectStoreFactoryBean()
    {
    }

    /**
     * Set a delegate to create the object stores in a non-default way
     */
    public static void setDelegate(DefaultObjectStoreFactory theDelegate)
    {
        delegate = theDelegate;
    }

    public static ObjectStore<Serializable> createDefaultInMemoryObjectStore()
    {
        ObjectStore<Serializable> store;
        if (delegate != null)
        {
            return delegate.createDefaultInMemoryObjectStore();
        }
        return new SimpleMemoryObjectStore<Serializable>();
    }

    public static ObjectStore<Serializable> createDefaultPersistentObjectStore()
    {
        if (delegate != null)
        {
            return delegate.createDefaultPersistentObjectStore();
        }
        return new QueuePersistenceObjectStore<Serializable>();
    }
}
