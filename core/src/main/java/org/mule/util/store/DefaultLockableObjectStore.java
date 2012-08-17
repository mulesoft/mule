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

import org.mule.api.store.LockableObjectStore;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.util.lock.MuleEntryLocker;

import java.io.Serializable;

public class DefaultLockableObjectStore<T extends Serializable> implements LockableObjectStore<T>
{
    private ObjectStore<T> objectStore;
    private MuleEntryLocker entryLocker;

    public DefaultLockableObjectStore(ObjectStore<T> objectStore, MuleEntryLocker muleEntryLocker)
    {
        this.objectStore = objectStore;
        this.entryLocker = muleEntryLocker;
    }

    @Override
    public void lockEntry(Serializable key)
    {
        this.entryLocker.lock(key);
    }

    @Override
    public void releaseEntry(Serializable key)
    {
        this.entryLocker.release(key);
    }

    @Override
    public boolean contains(Serializable key) throws ObjectStoreException
    {
        return objectStore.contains(key);
    }

    @Override
    public void store(Serializable key, T value) throws ObjectStoreException
    {
        objectStore.store(key,value);
    }

    @Override
    public T retrieve(Serializable key) throws ObjectStoreException
    {
        return objectStore.retrieve(key);
    }

    @Override
    public T remove(Serializable key) throws ObjectStoreException
    {
        return objectStore.remove(key);
    }

    @Override
    public boolean isPersistent()
    {
        return objectStore.isPersistent();
    }
}
