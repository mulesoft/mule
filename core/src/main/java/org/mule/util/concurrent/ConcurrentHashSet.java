/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.concurrent;

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166 Expert Group and
 * released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

import java.io.IOException;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class ConcurrentHashSet/* <E> */extends AbstractSet/* <E> */implements Set/* <E> */, Serializable
{
    private static final long serialVersionUID = 2454657854757543876L;

    private final ConcurrentHashMap/* <E, Boolean> */_map;
    private transient Set/* <E> */_keySet;

    public ConcurrentHashSet()
    {
        _map = new ConcurrentHashMap/* <E, Boolean> */();
        _keySet = _map.keySet();
    }

    public ConcurrentHashSet(int initialCapacity)
    {
        _map = new ConcurrentHashMap/* <E, Boolean> */(initialCapacity);
        _keySet = _map.keySet();
    }

    public ConcurrentHashSet(int initialCapacity, float loadFactor, int concurrencyLevel)
    {
        _map = new ConcurrentHashMap/* <E, Boolean> */(initialCapacity, loadFactor, concurrencyLevel);
        _keySet = _map.keySet();
    }

    public int size()
    {
        return _map.size();
    }

    public boolean isEmpty()
    {
        return _map.isEmpty();
    }

    public boolean contains(Object o)
    {
        return _map.containsKey(o);
    }

    public Iterator/* <E> */iterator()
    {
        return _keySet.iterator();
    }

    public Object[] toArray()
    {
        return _keySet.toArray();
    }

    public/* <T> T[] */Object[] toArray(Object[]/* T[] */a)
    {
        return _keySet.toArray(a);
    }

    public boolean add(Object/* E */e)
    {
        return _map.put(e, Boolean.TRUE) == null;
    }

    public boolean remove(Object o)
    {
        return _map.remove(o) != null;
    }

    public boolean removeAll(Collection/* <?> */c)
    {
        return _keySet.removeAll(c);
    }

    public boolean retainAll(Collection/* <?> */c)
    {
        return _keySet.retainAll(c);
    }

    public void clear()
    {
        _map.clear();
    }

    public boolean equals(Object o)
    {
        return _keySet.equals(o);
    }

    public int hashCode()
    {
        return _keySet.hashCode();
    }

    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException
    {
        s.defaultReadObject();
        _keySet = _map.keySet();
    }

}
