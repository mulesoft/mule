/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.store;

import java.io.Serializable;

public interface ObjectStoreManager
{
    /**
     * Return the partition of the default in-memory store with the given name, creating it
     * if necessary.
     */
    <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name);

    /**
     * Return the partition of the default in-memory or persistent store with the given name, creating it
     * if necessary.
     */
    <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name, boolean isPersistent);

    /**
     * Return the monitored partition of the default in-memory or persistent store with the given name, creating it
     * if necessary.
     */
    <T extends ObjectStore<? extends Serializable>> T getObjectStore(String name,
        boolean isPersistent, int maxEntries, int entryTTL, int expirationInterval);

    /**
     * Delete all objects from the partition
     */
    void disposeStore(ObjectStore<? extends Serializable> store) throws ObjectStoreException;
}
