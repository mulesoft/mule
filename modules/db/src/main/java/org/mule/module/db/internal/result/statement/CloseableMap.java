/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.result.statement;

import org.mule.api.Closeable;
import org.mule.api.MuleException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Defines a {@link Map} that will close any contained {@link Closeable} value
 */
public class CloseableMap<K, V> implements Map<K,V>, Closeable
{

    protected static final Log logger = LogFactory.getLog(CloseableMap.class);

    private Map<K,V> delegate = new HashMap<K, V>();

    @Override
    public void close() throws MuleException
    {
        for (V value : delegate.values())
        {
            if (value instanceof Closeable)
            {
                try
                {
                    ((Closeable) value).close();
                }
                catch (MuleException e)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Error closing map element", e);
                    }
                }
            }
        }

    }

    @Override
    public int size()
    {
        return delegate.size();
    }

    @Override
    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        return delegate.containsValue(value);
    }

    @Override
    public V get(Object key)
    {
        return delegate.get(key);
    }

    @Override
    public V put(K key, V value)
    {
        return delegate.put(key, value);
    }

    @Override
    public V remove(Object key)
    {
        return delegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map)
    {
        delegate.putAll(map);
    }

    @Override
    public void clear()
    {
        delegate.clear();
    }

    @Override
    public Set<K> keySet()
    {
        return delegate.keySet();
    }

    @Override
    public Collection<V> values()
    {
        return delegate.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet()
    {
        return delegate.entrySet();
    }
}
