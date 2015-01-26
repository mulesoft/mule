/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Represents a Map from String to {@link T} where the key's case is not taken into account when looking for it, but
 * remembered when the key set is retrieved from the map.
 * The backing map used will be an instance of the given class passed to the constructor. This allows to make case
 * insensitive different kinds of Map implementations, particularly a ConcurrentHashMap, used to achieve high concurrence.
 * <p/>
 * When a key/value pair is put in the map the key case is remembered so when the key set or the entry set is retrieved
 * the correct case is returned. This is useful to store, for example, camel case keys. However, two keys that only
 * differ in their case will be assumed to be the same key and only one value (the last) will be kept.
 * Note: as this map uses a provided class to create the backing map, key rewrite is not ensured. It is possible that
 * when redefining a value associated to a key, the key case won't be overwritten and the already existing key case
 * will remains in the key set and entry set.
 *
 * @param <T> The class of the values referenced in the map.
 *
 * @since 3.6.0
 */
public class CaseInsensitiveMapWrapper<T> implements Map<String, T>, Serializable
{

    private final Map<CaseInsensitiveMapKey, T> baseMap;

    public CaseInsensitiveMapWrapper(Class<? extends Map> mapClass, Object... parameters)
    {
        try
        {
            baseMap = ClassUtils.instanciateClass(mapClass, parameters);
        }
        catch (Exception e)
        {
            throw new RuntimeException(String.format("Can not create an instance of %s", mapClass.getCanonicalName()), e);
        }
    }

    @Override
    public int size()
    {
        return baseMap.size();
    }

    @Override
    public boolean isEmpty()
    {
        return baseMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        return baseMap.containsKey(new CaseInsensitiveMapKey(key));
    }

    @Override
    public boolean containsValue(Object value)
    {
        return baseMap.containsValue(value);
    }

    @Override
    public T get(Object key)
    {
        return baseMap.get(new CaseInsensitiveMapKey(key));
    }

    @Override
    public T put(String key, T value)
    {
        return baseMap.put(new CaseInsensitiveMapKey(key), value);
    }

    @Override
    public T remove(Object key)
    {
        return baseMap.remove(new CaseInsensitiveMapKey(key));
    }

    @Override
    public void putAll(Map<? extends String, ? extends T> other)
    {
        if (other instanceof CaseInsensitiveMapWrapper)
        {
            baseMap.putAll(((CaseInsensitiveMapWrapper) other).baseMap);
        }
        else
        {
            for (Map.Entry<? extends String, ? extends T> otherEntry : other.entrySet())
            {
                put(otherEntry.getKey(), otherEntry.getValue());
            }
        }
    }

    @Override
    public void clear()
    {
        baseMap.clear();
    }

    @Override
    public Set<String> keySet()
    {
        return new KeySet(baseMap.keySet());
    }

    @Override
    public Collection<T> values()
    {
        return baseMap.values();
    }

    @Override
    public Set<Entry<String, T>> entrySet()
    {
        return new EntrySet<>(baseMap.entrySet());
    }


    private static class CaseInsensitiveMapKey implements Serializable
    {

        private final String key;
        private final String keyLowerCase;
        private final int keyHash;

        public CaseInsensitiveMapKey(Object key)
        {
            this.key = key.toString();
            keyLowerCase = this.key.toLowerCase();
            keyHash = keyLowerCase.hashCode();
        }

        public String getKey()
        {
            return key;
        }

        @Override
        public int hashCode()
        {
            return keyHash;
        }

        @Override
        public boolean equals(Object obj)
        {
            return (obj instanceof CaseInsensitiveMapKey) &&
                   keyLowerCase.equals(((CaseInsensitiveMapKey) obj).keyLowerCase);
        }
    }


    private static class KeySet extends AbstractConverterSet<CaseInsensitiveMapKey, String>
    {

        public KeySet(Set<CaseInsensitiveMapKey> keys)
        {
            super(keys);
        }

        @Override
        protected Iterator<String> createIterator(Set<CaseInsensitiveMapKey> keys)
        {
            return new KeyIterator(keys);
        }
    }

    private static class EntrySet<T> extends AbstractConverterSet<Entry<CaseInsensitiveMapKey, T>, Entry<String, T>>
    {

        public EntrySet(Set<Map.Entry<CaseInsensitiveMapKey, T>> entries)
        {
            super(entries);
        }

        @Override
        protected Iterator<Entry<String, T>> createIterator(Set<Entry<CaseInsensitiveMapKey, T>> entries)
        {
            return new EntryIterator<>(entries);
        }
    }

    private static abstract class AbstractConverterSet<A, B> extends AbstractSet<B>
    {

        private final Set<A> aSet;

        public AbstractConverterSet(Set<A> set)
        {
            this.aSet = set;
        }

        @Override
        public int size()
        {
            return aSet.size();
        }

        @Override
        public Iterator<B> iterator()
        {
            return createIterator(aSet);
        }

        protected abstract Iterator<B> createIterator(Set<A> aSet);
    }


    private static class KeyIterator extends AbstractConverterIterator<CaseInsensitiveMapKey, String>
    {

        public KeyIterator(Set<CaseInsensitiveMapKey> keys)
        {
            super(keys);
        }

        @Override
        protected String convert(CaseInsensitiveMapKey next)
        {
            return next.getKey();
        }
    }

    private static class EntryIterator<T> extends AbstractConverterIterator<Entry<CaseInsensitiveMapKey, T>, Entry<String, T>>
    {

        public EntryIterator(Set<Map.Entry<CaseInsensitiveMapKey, T>> entries)
        {
            super(entries);
        }

        @Override
        protected Entry<String, T> convert(Entry<CaseInsensitiveMapKey, T> next)
        {
            return new AbstractMap.SimpleEntry<>(next.getKey().getKey(), next.getValue());
        }
    }

    private static abstract class AbstractConverterIterator<A, B> implements Iterator<B>
    {

        private final Iterator<A> aIterator;

        public AbstractConverterIterator(Set<A> set)
        {
            aIterator = set.iterator();
        }

        @Override
        public boolean hasNext()
        {
            return aIterator.hasNext();
        }

        @Override
        public final void remove()
        {
            aIterator.remove();
        }

        @Override
        public final B next()
        {
            return convert(aIterator.next());
        }

        protected abstract B convert(A next);
    }
}