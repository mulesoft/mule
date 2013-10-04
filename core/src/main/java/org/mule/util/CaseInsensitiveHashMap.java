/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.collections.map.AbstractHashedMap;

/**
 * A case-insensitive <code>Map</code>.
 * <p/>
 * As entries are added to the map, keys hash values are lowercase hash codes of the key.  the
 * Real key case is preserved.
 * <p/>
 * <p/>
 * The <code>keySet()</code> method returns all keys in their original case
 * <p/>
 * <strong>Note that CaseInsensitiveMap is not synchronized and is not thread-safe.</strong>
 * If you wish to use this map from multiple threads concurrently, you must use
 * appropriate synchronization. The simplest approach is to wrap this map
 * using {@link java.util.Collections#synchronizedMap(Map)}. This class may throw
 * exceptions when accessed by concurrent threads without synchronization.
 *
 * @since 3.0.0
 */
public class CaseInsensitiveHashMap extends AbstractHashedMap implements Serializable
{
    /**
     * Serialisation version
     */
    private static final long serialVersionUID = -7074633917369299456L;

    /**
     * Constructs a new empty map with default size and load factor.
     */
    public CaseInsensitiveHashMap()
    {
        super(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_THRESHOLD);
    }


    /**
     * Constructs a new, empty map with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity
     * @throws IllegalArgumentException if the initial capacity is less than one
     */
    public CaseInsensitiveHashMap(int initialCapacity) throws IllegalArgumentException
    {
        super(initialCapacity);
    }

    /**
     * Constructs a new, empty map with the specified initial capacity and
     * load factor.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is less than one
     * @throws IllegalArgumentException if the load factor is less than zero
     */
    public CaseInsensitiveHashMap(int initialCapacity, float loadFactor) throws IllegalArgumentException
    {
        super(initialCapacity, loadFactor);
    }

    /**
     * Constructor copying elements from another map.
     * <p/>
     * Keys will be converted to lower case strings, which may cause
     * some entries to be removed (if string representation of keys differ
     * only by character case).
     *
     * @param map the map to copy
     * @throws NullPointerException if the map is null
     */
    public CaseInsensitiveHashMap(Map map)
    {
        super(map);
    }

    /**
     * Creates a hash value from the lower case value of the key. The same function will be used when querying
     * a value in the map also
     *
     * @param key the key value to hash
     * @return a hash value for the lower case key
     */
    @Override
    protected int hash(Object key)
    {
        return super.hash(key.toString().toLowerCase());
    }

    /**
     * Overloads the default behaviour to compare the keys without case sensitivity
     *
     * @param key1 the first key
     * @param key2 the key to compare against
     * @return true is the keys match
     */
    @Override
    protected boolean isEqualKey(Object key1, Object key2)
    {
        if (key1 instanceof String && key2 instanceof String)
        {
            return (((String) key1).equalsIgnoreCase((String) key2));
        }
        else
        {
            return super.isEqualKey(key1, key2);
        }
    }

    //-----------------------------------------------------------------------

    /**
     * Clones the map without cloning the keys or values.
     *
     * @return a shallow clone
     */
    @Override
    public Object clone()
    {
        return super.clone();
    }

    /**
     * Write the map out using a custom routine.
     */
    private void writeObject(ObjectOutputStream out) throws IOException
    {
        out.defaultWriteObject();
        doWriteObject(out);
    }

    /**
     * Read the map in using a custom routine.
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        doReadObject(in);
    }
}
