/*
 * $Id$
 * --------------------------------------------------------------------------------------
 *
 * (c) 2003-2010 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
