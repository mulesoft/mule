/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.concurrent;

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166 Expert Group and
 * released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashSet/* <E> */extends AbstractSet/* <E> */implements Set/* <E> */, Serializable
{
    private static final long serialVersionUID = 2454657854757543876L;

    private final Map/* <E, Boolean> */map;
    private transient Set/* <E> */keySet;

    public ConcurrentHashSet()
    {
        map = new ConcurrentHashMap/* <E, Boolean> */();
        keySet = map.keySet();
    }

    public ConcurrentHashSet(int initialCapacity)
    {
        map = new ConcurrentHashMap/* <E, Boolean> */(initialCapacity);
        keySet = map.keySet();
    }

    public ConcurrentHashSet(int initialCapacity, float loadFactor, int concurrencyLevel)
    {
        map = new ConcurrentHashMap/* <E, Boolean> */(initialCapacity, loadFactor, concurrencyLevel);
        keySet = map.keySet();
    }

    public int size()
    {
        return map.size();
    }

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    public boolean contains(Object o)
    {
        return map.containsKey(o);
    }

    public Iterator/* <E> */iterator()
    {
        return keySet.iterator();
    }

    public Object[] toArray()
    {
        return keySet.toArray();
    }

    public/* <T> T[] */Object[] toArray(Object[]/* T[] */a)
    {
        return keySet.toArray(a);
    }

    public boolean add(Object/* E */e)
    {
        return map.put(e, Boolean.TRUE) == null;
    }

    public boolean remove(Object o)
    {
        return map.remove(o) != null;
    }

    public boolean removeAll(Collection/* <?> */c)
    {
        return keySet.removeAll(c);
    }

    public boolean retainAll(Collection/* <?> */c)
    {
        return keySet.retainAll(c);
    }

    public void clear()
    {
        map.clear();
    }

    public boolean equals(Object o)
    {
        return keySet.equals(o);
    }

    public int hashCode()
    {
        return keySet.hashCode();
    }

    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException
    {
        s.defaultReadObject();
        keySet = map.keySet();
    }

}
