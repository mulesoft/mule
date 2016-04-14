/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.api.MuleRuntimeException;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreException;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapts the object store interface to a map interface so the client doesn't have to deal
 * with all the ObjectStoreExceptions thrown by ObjectStore.
 *
 * This class provides limited functionality from the Map interface. It does not support some methods
 * (see methods javadoc) that can have a big impact in performance due the underlying object store being used.
 *
 * The object store provided will be access for completing the map operations but the whole lifecycle of the
 * provided object store must be handled by the user.
 *
 * Operations of this map are not thread safe so the user must synchronize access to this map properly.
 *
 * @param <ValueType> store type
 */
public class ObjectStoreToMapAdapter<ValueType extends Serializable> implements Map<Serializable, ValueType>
{

    private final ListableObjectStore<ValueType> objectStore;

    public ObjectStoreToMapAdapter(final ListableObjectStore<ValueType> listableObjectStore)
    {
        this.objectStore = listableObjectStore;
    }

    @Override
    public int size()
    {
        try
        {
            return this.objectStore.allKeys().size();
        }
        catch (ObjectStoreException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    @Override
    public boolean isEmpty()
    {
        try
        {
            return this.objectStore.allKeys().isEmpty();
        }
        catch (ObjectStoreException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    @Override
    public boolean containsKey(Object key)
    {
        try
        {
            return this.objectStore.contains((Serializable) key);
        }
        catch (ObjectStoreException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    @Override
    public boolean containsValue(Object value)
    {
        throw new UnsupportedOperationException("Map adapter for object store does not support contains value");
    }

    @Override
    public ValueType get(Object key)
    {
        try
        {
            if (!this.objectStore.contains((Serializable) key))
            {
                return null;
            }
            return this.objectStore.retrieve((Serializable) key);
        }
        catch (ObjectStoreException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    @Override
    public ValueType put(Serializable key, ValueType value)
    {
        ValueType previousValue = null;
        try
        {
            if (this.objectStore.contains(key))
            {
                previousValue = objectStore.retrieve(key);
                objectStore.remove(key);
            }
            if (value != null)
            {
                objectStore.store(key, value);
            }
            return previousValue;
        }
        catch (ObjectStoreException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    @Override
    public ValueType remove(Object key)
    {
        try
        {
            if (objectStore.contains((Serializable) key))
            {
                return objectStore.remove((Serializable) key);
            }
        }
        catch (ObjectStoreException e)
        {
            throw new MuleRuntimeException(e);
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends Serializable, ? extends ValueType> mapToAdd)
    {
        for (Serializable key : mapToAdd.keySet())
        {
            put(key, mapToAdd.get(key));
        }
    }

    @Override
    public void clear()
    {
        try
        {
            objectStore.clear();
        }
        catch (ObjectStoreException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    @Override
    public Set<Serializable> keySet()
    {
        try
        {
            final List<Serializable> allKeys = objectStore.allKeys();
            return new HashSet(allKeys);
        }
        catch (ObjectStoreException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    /**
     * This method is not supported for performance reasons
     */
    @Override
    public Collection<ValueType> values()
    {
        throw new UnsupportedOperationException("ObjectStoreToMapAdapter does not support values() method");
    }

    /**
     * This method is not supported for performance reasons
     */
    @Override
    public Set<Entry<Serializable, ValueType>> entrySet()
    {
        throw new UnsupportedOperationException("ObjectStoreToMapAdapter does not support entrySet() method");
    }

}
