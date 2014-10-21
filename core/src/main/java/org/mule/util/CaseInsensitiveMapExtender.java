/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
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
 * insensitive different kinds of Map implementations, particular a ConcurrentHashMap, used to achieve high concurrence.
 * <p>
 * When a key/value pair is put in the map the key case is remembered so when the key set or the entry set is retrieved
 * the correct case is returned. This is useful to store, for example, camel case keys. However, two keys that only
 * differ in their case will be assumed to be the same key and only one value (the last) will be kept.
 * Note: as this map uses a provided class to create the backing map, key rewritten is not ensured. It is possible that
 * when redefining a value associated to a key, the key case won't be overwritten and the already existing key case
 * will remains in the key set and entry set.
 *
 * @param <T> The class of the values referenced in the map.
 */
public class CaseInsensitiveMapExtender<T> implements Map<String, T>, Serializable
{

    Map<CaseInsensitiveMapKey, T> baseMap;

    public CaseInsensitiveMapExtender(Class<? extends Map> mapClass, Object... parameters) throws Exception
    {
        baseMap = create(mapClass, parameters);
    }

    private static <C> C create(Class<C> cClass, Object... parameters) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        Class[] parametersClasses = new Class[parameters.length];
        for (int i = 0; i < parameters.length; i++)
        {
            parametersClasses[i] = parameters[i].getClass();
        }
        return cClass.getConstructor(parametersClasses).newInstance(parameters);
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
        return baseMap.containsKey(CaseInsensitiveMapKey.instance(key));
    }

    @Override
    public boolean containsValue(Object value)
    {
        return baseMap.containsValue(value);
    }

    @Override
    public T get(Object key)
    {
        return baseMap.get(CaseInsensitiveMapKey.instance(key));
    }

    @Override
    public T put(String key, T value)
    {
        if (value == null)
        {
            return remove(key);
        }
        return baseMap.put(CaseInsensitiveMapKey.instance(key), value);
    }

    @Override
    public T remove(Object key)
    {
        return baseMap.remove(CaseInsensitiveMapKey.instance(key));
    }

    @Override
    public void putAll(Map<? extends String, ? extends T> other)
    {
        if (other instanceof CaseInsensitiveMapExtender)
        {
            baseMap.putAll(((CaseInsensitiveMapExtender) other).baseMap);
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

}

class CaseInsensitiveMapKey implements Serializable
{

    private String key;
    private String keyLowerCase;
    private int keyHash;

    public static CaseInsensitiveMapKey instance(Object key)
    {
        return new CaseInsensitiveMapKey(key.toString());
    }

    private CaseInsensitiveMapKey(String key)
    {
        this.key = key;
        this.keyLowerCase = key.toLowerCase();
        this.keyHash = this.keyLowerCase.hashCode();
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
        return (obj != null && obj instanceof CaseInsensitiveMapKey) ||
            keyLowerCase.equals(((CaseInsensitiveMapKey) obj).keyLowerCase);
    }
}

class KeySet extends AbstractBackedSet<CaseInsensitiveMapKey, String>
{

    public KeySet(Set<CaseInsensitiveMapKey> keys)
    {
        super(keys);
    }

    @Override
    public Iterator<String> iterator()
    {
        return new KeyIterator(backingSet);
    }
}

class KeyIterator extends AbstractBackedIterator<CaseInsensitiveMapKey, String>
{

    public KeyIterator(Set<CaseInsensitiveMapKey> keys)
    {
        super(keys);
    }

    @Override
    public final String next()
    {
        return backingIterator.next().getKey();
    }
}

class EntrySet<T> extends AbstractBackedSet<Map.Entry<CaseInsensitiveMapKey, T>, Map.Entry<String, T>>
{

    public EntrySet(Set<Map.Entry<CaseInsensitiveMapKey, T>> entries)
    {
        super(entries);
    }

    @Override
    public Iterator<Map.Entry<String, T>> iterator()
    {
        return new EntryIterator<T>(backingSet);
    }
}

class EntryIterator<T> extends AbstractBackedIterator<Map.Entry<CaseInsensitiveMapKey, T>, Map.Entry<String, T>>
{

    public EntryIterator(Set<Map.Entry<CaseInsensitiveMapKey, T>> entries)
    {
        super(entries);
    }

    @Override
    public final Map.Entry<String, T> next()
    {
        Map.Entry<CaseInsensitiveMapKey, T> backingEntry = backingIterator.next();
        return new AbstractMap.SimpleEntry<>(backingEntry.getKey().getKey(), backingEntry.getValue());
    }
}

abstract class AbstractBackedSet<A, B> extends AbstractSet<B>
{

    Set<A> backingSet;

    public AbstractBackedSet(Set<A> set)
    {
        this.backingSet = set;
    }

    @Override
    public int size()
    {
        return backingSet.size();
    }
}

abstract class AbstractBackedIterator<A, B> implements Iterator<B>
{

    Iterator<A> backingIterator;

    public AbstractBackedIterator(Set<A> set)
    {
        backingIterator = set.iterator();
    }

    @Override
    public boolean hasNext()
    {
        return backingIterator.hasNext();
    }

    @Override
    public final void remove()
    {
        backingIterator.remove();
    }
}
