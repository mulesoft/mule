/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.el.context;

import org.mule.api.MuleRuntimeException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;

public abstract class AbstractMapContext<K, V> implements Map<K, V>
{

    public void putAll(Map<? extends K, ? extends V> m)
    {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear()
    {
        for (K key : keySet())
        {
            try
            {
                remove(key);
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(e);
            }
        }
    }

    public int size()
    {
        return keySet().size();
    }

    public boolean isEmpty()
    {
        return keySet().isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        return keySet().contains(key);
    }

    @Override
    public Collection<V> values()
    {
        List<V> values = new ArrayList<V>(size());
        for (K key : keySet())
        {
            values.add(get(key));
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet()
    {
        Set<java.util.Map.Entry<K, V>> entrySet = new HashSet<java.util.Map.Entry<K, V>>();
        for (K key : keySet())
        {
            entrySet.add(new DefaultMapEntry(key, get(key)));
        }
        return entrySet;
    }

    @Override
    public boolean containsValue(Object value)
    {
        for (K key : keySet())
        {
            if (value.equals(get(key)))
            {
                return true;
            }
        }
        return false;
    }

}
