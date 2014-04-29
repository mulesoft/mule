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
 * of {@link CaseInsensitiveHashMap}. <br>
 * <b>Note:</b> In this {@link Map} implementation {@link #keySet()}, {@link #values()} and
 * {@link #entrySet()} return unmodifiable {@link Collection}'s. In order to mutate this Map implementation
 * you should use {@link #put(Object, Object)}, {@link #putAll(Map)}, {@link #remove(Object)} and
 * {@link #clear()} methods.<br>
 * This implementation is not thread-safe.
 */
public class CopyOnWriteCaseInsensitiveMap<K, V> implements Map<K, V>, Serializable
{

    private static final long serialVersionUID = -2753436627413265538L;

    private Map<K, V> core;
    private transient Map<K, V> view;
    private transient boolean requiresCopy;

    @SuppressWarnings("unchecked")
    public CopyOnWriteCaseInsensitiveMap()
    {
        updateCore(new CaseInsensitiveHashMap());
    }

    private CopyOnWriteCaseInsensitiveMap(CopyOnWriteCaseInsensitiveMap<K, V> that)
    {
        updateCore(that.core);
        this.requiresCopy = true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public CopyOnWriteCaseInsensitiveMap<K, V> clone()
    {
        try
        {
            return new CopyOnWriteCaseInsensitiveMap(this);
        }
        finally
        {
            requiresCopy = true;
        }
    }

    @SuppressWarnings("unchecked")
    private void copy()
    {
        if (requiresCopy)
        {
            updateCore(new CaseInsensitiveHashMap(core));
            requiresCopy = false;
        }
    }

    @Override
    public int size()
    {
        return core.size();
    }

    @Override
    public boolean isEmpty()
    {
        return core.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        return core.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        return core.containsValue(value);
    }

    @Override
    public V get(Object key)
    {
        return core.get(key);
    }

    @Override
    public V put(K key, V value)
    {
        copy();
        return core.put(key, value);
    }

    @Override
    public V remove(Object key)
    {
        copy();
        return core.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> t)
    {
        copy();
        core.putAll(t);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void clear()
    {
        updateCore(new CaseInsensitiveHashMap());
    }

    @Override
    public Set<K> keySet()
    {
        return view.keySet();
    }

    @Override
    public Collection<V> values()
    {
        return view.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet()
    {
        return view.entrySet();
    }

    @Override
    public String toString()
    {
        return core.toString();
    }

    private void updateCore(Map<K, V> core)
    {
        this.core = core;
        this.view = Collections.unmodifiableMap(core);
    }

    /**
     * After deserialization we can just use unserialized original map directly.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        this.view = Collections.unmodifiableMap(core);
    }

}
