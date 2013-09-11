/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.util;

import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class FakeObjectStore<T extends Serializable> implements ObjectStore<T>
{

    Map<Serializable, T> store = new HashMap<Serializable, T>();

    @Override
    public boolean contains(Serializable key) throws ObjectStoreException
    {
        return store.containsKey(key);
    }
    
    @Override
    public void clear() throws ObjectStoreException
    {
        this.store.clear();
    }

    @Override
    public void store(Serializable key, T value) throws ObjectStoreException
    {
        if (store.containsKey(key))
        {
            throw new ObjectAlreadyExistsException(new Exception());
        }
        store.put(key, value);
    }

    @Override
    public T retrieve(Serializable key) throws ObjectStoreException
    {
        if (!store.containsKey(key))
        {
            throw new ObjectDoesNotExistException(new Exception());
        }
        return store.get(key);
    }

    @Override
    public T remove(Serializable key) throws ObjectStoreException
    {
        if (!store.containsKey(key))
        {
            throw new ObjectAlreadyExistsException(new Exception());
        }
        return store.remove(key);
    }

    @Override
    public boolean isPersistent()
    {
        return false;
    }
}
