/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


