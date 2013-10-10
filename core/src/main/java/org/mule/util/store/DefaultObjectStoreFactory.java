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
 * Create the default object stores
 */
public interface DefaultObjectStoreFactory
{
    ObjectStore<Serializable> createDefaultInMemoryObjectStore();

    ObjectStore<Serializable> createDefaultPersistentObjectStore();

    QueueStore<Serializable> createDefaultInMemoryQueueStore();

    QueueStore<Serializable> createDefaultPersistentQueueStore();

    ObjectStore<Serializable> createDefaultUserObjectStore();
}
