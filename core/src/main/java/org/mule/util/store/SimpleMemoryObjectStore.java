/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
