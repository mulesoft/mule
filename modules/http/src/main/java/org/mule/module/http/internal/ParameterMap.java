/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParameterMap implements Map<String, Object>, Serializable
{

    private final Map paramsMap;

    public ParameterMap(final Map paramsMap)
    {
        this.paramsMap = Collections.unmodifiableMap(paramsMap);
    }

    public ParameterMap()
    {
        this.paramsMap = new LinkedHashMap();
    }

    @Override
    public int size()
    {
        return paramsMap.size();
    }

    @Override
    public boolean isEmpty()
    {
        return paramsMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        return paramsMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        return paramsMap.containsValue(value);
    }

    @Override
    public String get(Object key)
    {
        final Object value = paramsMap.get(key);
        if (value != null)
        {
            return ((List<String>)value).get(0);
        }
        return null;
    }

    public List<String> getAsList(Object key)
    {
        return paramsMap.containsKey(key) ? Collections.unmodifiableList((List<? extends String>) paramsMap.get(key)) : Collections.<String>emptyList();
    }

    @Override
    public Object put(String key, Object value)
    {
        List<Object> previousValue = (List<Object>) paramsMap.get(key);
        List<Object> newValue = previousValue;
        if (previousValue != null)
        {
            previousValue = new ArrayList<>(previousValue);
        }
        else
        {
            newValue = new ArrayList<>();
        }
        newValue.add(value);
        paramsMap.put(key, newValue);
        return previousValue;
    }

    public ParameterMap putAndReturn(String key, Object value)
    {
        put(key, value);
        return this;
    }

    @Override
    public Object remove(Object key)
    {
        return paramsMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m)
    {
        paramsMap.putAll(m);
    }

    @Override
    public void clear()
    {
        paramsMap.clear();
    }

    @Override
    public Set<String> keySet()
    {
        return paramsMap.keySet();
    }

    @Override
    public Collection<Object> values()
    {
        return paramsMap.values();
    }

    @Override
    public Set<Entry<String,Object>> entrySet()
    {
        return paramsMap.entrySet();
    }

    @Override
    public boolean equals(Object o)
    {
        return paramsMap.equals(o);
    }

    @Override
    public int hashCode()
    {
        return paramsMap.hashCode();
    }

    public Map<String, Collection<Object>> toCollectionMap()
    {
        return Collections.unmodifiableMap(paramsMap);
    }
}
