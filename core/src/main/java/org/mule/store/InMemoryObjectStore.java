/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.store;

import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple implementation of the ObjectStore interface which is backed by an in-memory hashmap.
 * The hashmap will be created/destoyed when calls are made to open()/close() respectively.
 */
public class InMemoryObjectStore<T extends Serializable> implements ListableObjectStore<T>
{
    private Map<Serializable, T> map;
    
    public boolean contains(Serializable key) throws ObjectStoreException
    {
        return map.containsKey(key);
    }

    public void store(Serializable key, T value) throws ObjectStoreException
    {
        map.put(key, value);
    }

    public T retrieve(Serializable key) throws ObjectStoreException
    {
        return map.get(key);
    }

    public T remove(Serializable key) throws ObjectStoreException
    {
        return map.remove(key);
    }

    public List<Serializable> allKeys() throws ObjectStoreException
    {
        List<Serializable> list = new ArrayList<Serializable>();
        list.addAll(map.keySet());
        return list;
    }

    public void open() throws ObjectStoreException
    {
        map = Collections.synchronizedMap(new HashMap<Serializable, T>());
    }

    public void close() throws ObjectStoreException
    {
        map.clear();
        map = null;
    }
}


