/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal;

import static java.util.Arrays.toString;
import static java.util.Collections.unmodifiableMap;

import org.mule.module.http.api.HttpParameters;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParameterMap implements HttpParameters, Serializable
{

    private final Map<String, LinkedList<String>> paramsMap;

    public ParameterMap(final Map paramsMap)
    {
        this.paramsMap = unmodifiableMap(paramsMap);
    }

    public ParameterMap()
    {
        this.paramsMap = new LinkedHashMap();
    }

    public ParameterMap toImmutableParameterMap()
    {
        return new ParameterMap(this.paramsMap);
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
            LinkedList<String> values = (LinkedList<String>) value;
            return values.getLast();
        }
        return null;
    }

    public List<String> getAll(String key)
    {
        return paramsMap.containsKey(key) ? Collections.unmodifiableList(paramsMap.get(key)) : Collections.<String>emptyList();
    }

    @Override
    public String put(String key, String value)
    {
        LinkedList<String> previousValue = paramsMap.get(key);
        LinkedList<String> newValue = previousValue;
        if (previousValue != null)
        {
            previousValue = new LinkedList<>(previousValue);
        }
        else
        {
            newValue = new LinkedList<>();
        }
        newValue.add(value);
        paramsMap.put(key, newValue);
        if (previousValue == null || previousValue.isEmpty())
        {
            return null;
        }
        return previousValue.getFirst();
    }

    public String remove(Object key)
    {
        Collection<String> values = paramsMap.remove(key);
        if (values != null)
        {
            return values.iterator().next();
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> aMap)
    {
        for (String key : aMap.keySet())
        {
            LinkedList<String> values = new LinkedList<>();
            values.add(aMap.get(key));
            paramsMap.put(key, values);
        }
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
    public Collection<String> values()
    {
        ArrayList<String> values = new ArrayList<>();
        for (String key : paramsMap.keySet())
        {
            values.add(paramsMap.get(key).getLast());
        }
        return values;
    }

    @Override
    public Set<Entry<String,String>> entrySet()
    {
        HashSet<Entry<String, String>> entries = new HashSet<>();
        for (String key : paramsMap.keySet())
        {
            entries.add(new AbstractMap.SimpleEntry<>(key, paramsMap.get(key).getLast()));
        }
        return entries;
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

    public Map<String, ? extends List<String>> toListValuesMap()
    {
        return unmodifiableMap(paramsMap);
    }

    @Override
    public String toString()
    {
        return "ParameterMap{" +
               Arrays.toString(paramsMap.entrySet().toArray()) +
               '}';
    }
}
