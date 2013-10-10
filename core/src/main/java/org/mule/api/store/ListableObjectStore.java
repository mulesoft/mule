/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.store;

import java.io.Serializable;
import java.util.List;

public interface ListableObjectStore<T extends Serializable> extends ObjectStore<T>
{
    /**
     * Open the underlying store.
     *
     * @throws ObjectStoreException if an exception occurred while opening the underlying store.
     */
    void open() throws ObjectStoreException;

    /**
     * Close the underlying store.
     *
     * @throws ObjectStoreException if an exception occurred while closing the underlying store.
     */
    void close() throws ObjectStoreException;

    /**
     * @return list containing all keys that this object store currently holds values for.
     *
     * @throws ObjectStoreException if an exception occurred while collecting the list of all keys.
     */
    List<Serializable> allKeys() throws ObjectStoreException;
}


