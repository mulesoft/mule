/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.config.i18n.CoreMessages;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleMemoryObjectStore<T extends Serializable> extends AbstractObjectStore<T>
    implements ListableObjectStore<T>
{
    private Map<Serializable, T> map = Collections.synchronizedMap(new HashMap<Serializable, T>());

    @Override
    public boolean isPersistent()
    {
        return false;
    }

    @Override
    protected boolean doContains(Serializable key)
    {
        return map.containsKey(key);
    }

    @Override
    protected void doStore(Serializable key, T value) throws ObjectStoreException
    {
        if (value == null)
        {
            throw new ObjectStoreException(CoreMessages.objectIsNull("value"));
        }

        map.put(key, value);
    }

    @Override
    protected T doRetrieve(Serializable key)
    {
        return map.get(key);
    }
    
    @Override
    public void clear() throws ObjectStoreException
    {
        this.map.clear();
    }

    @Override
    protected T doRemove(Serializable key)
    {
        return map.remove(key);
    }

    @Override
    public void open() throws ObjectStoreException
    {
        // this is a no-op
    }

    @Override
    public void close() throws ObjectStoreException
    {
        // this is a no-op
    }

    @Override
    public List<Serializable> allKeys() throws ObjectStoreException
    {
        return new ArrayList<Serializable>(map.keySet());
    }
}
