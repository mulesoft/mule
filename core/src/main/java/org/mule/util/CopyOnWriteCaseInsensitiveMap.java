/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link Map} that provides copy on write semantics while providing the case-insensitivity
 * of {@link CaseInsensitiveHashMap}.<br>
 * This implementation is not thread-safe.
 */
public class CopyOnWriteCaseInsensitiveMap<K, V> implements Map<K, V>, Serializable
{

    private static final long serialVersionUID = 4829196240419943077L;

    private Map<K, V> delegate;
    private boolean requiresCopy = true;

    @SuppressWarnings("unchecked")
    public CopyOnWriteCaseInsensitiveMap()
    {
        this.delegate = new CaseInsensitiveHashMap();
        requiresCopy = false;
    }

    public CopyOnWriteCaseInsensitiveMap(CopyOnWriteCaseInsensitiveMap<K,V> original)
    {
        this.delegate = Collections.unmodifiableMap(original);
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
        copy();
        return delegate.put(key, value);
    }

    @Override
    public V remove(Object key)
    {
        copy();
        return (V) delegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> t)
    {
        copy();
        delegate.putAll(t);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void clear()
    {
        copy();
        delegate = new CaseInsensitiveHashMap();
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

    @Override
    public String toString()
    {
        return delegate.toString();
    }

    @SuppressWarnings("unchecked")
    private void copy()
    {
        if (requiresCopy)
        {
            delegate = new CaseInsensitiveHashMap(delegate);
            requiresCopy = false;
        }
    }

    /**
     * After deserialization we can just use unserialized original map directly.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        requiresCopy = false;
    }
}
