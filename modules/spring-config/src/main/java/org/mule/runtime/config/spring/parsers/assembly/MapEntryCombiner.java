/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.assembly;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This is used internally by {@link org.mule.config.spring.parsers.assembly.DefaultBeanAssembler}
 * along with {@link org.mule.config.spring.parsers.collection.ChildSingletonMapDefinitionParser}.
 * It creates a map with a single key/value pair.  This may seem odd, but the result is not
 * manipulated within the assembler - that means that, unlike
 * {@link org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser}, this element
 * can contain nested values.  Note that most uses will set
 * {@link org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration#isCollection(String)}
 * so that several entries can be combined.
 */
public class MapEntryCombiner implements Map, Serializable
{

    public static final String KEY = "key";
    public static final String VALUE = "value";

    private Object key;
    private Object value;
    private Map cachedMerge = new HashMap();
    private boolean isMerged = false;

    private synchronized Map getCachedMerge()
    {
        if (!isMerged)
        {
            cachedMerge.put(key, value);
            isMerged = true;
        }
        return cachedMerge;
    }

    public Object getKey()
    {
        assertNotMerged();
        return key;
    }

    public void setKey(Object key)
    {
        assertNotMerged();
        this.key = key;
    }

    public Object getValue()
    {
        assertNotMerged();
        return value;
    }

    public void setValue(Object value)
    {
        assertNotMerged();
        this.value = value;
    }

    private synchronized void assertNotMerged()
    {
        if (isMerged)
        {
            throw new IllegalStateException("Maps have already been merged");
        }
    }

    // map delegates (except hashCode and equals)

    public int size()
    {
        return getCachedMerge().size();
    }

    public void clear()
    {
        getCachedMerge().clear();
    }

    public boolean isEmpty()
    {
        return getCachedMerge().isEmpty();
    }

    public boolean containsKey(Object key)
    {
        return getCachedMerge().containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        return getCachedMerge().containsValue(value);
    }

    public Collection values()
    {
        return getCachedMerge().values();
    }

    public void putAll(Map t)
    {
        getCachedMerge().putAll(t);
    }

    public Set entrySet()
    {
        return getCachedMerge().entrySet();
    }

    public Set keySet()
    {
        return getCachedMerge().keySet();
    }

    public Object get(Object key)
    {
        return getCachedMerge().get(key);
    }

    public Object remove(Object key)
    {
        return getCachedMerge().remove(key);
    }

    public Object put(Object key, Object value)
    {
        return getCachedMerge().put(key, value);
    }

}
