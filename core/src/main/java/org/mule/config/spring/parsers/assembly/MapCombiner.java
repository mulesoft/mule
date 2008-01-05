/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.assembly;

import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.Iterator;

public class MapCombiner implements Map
{

    public static final String LIST = "list"; // the setter/getter

    private List list;
    private Map map = new HashMap();
    private boolean merged = false;

    private synchronized Map getMap()
    {
        if (!merged)
        {
            for (Iterator maps = list.iterator(); maps.hasNext();)
            {
                map.putAll((Map) maps.next());
            }
            merged = true;
        }
        return map;
    }

    public void setList(List list)
    {
        assertNotMerged();
        this.list = list;
    }

    public List getList()
    {
        assertNotMerged();
        return list;
    }

    private synchronized void assertNotMerged()
    {
        if (merged)
        {
            throw new IllegalStateException("Maps have already been merged");
        }
    }

    // map delegates (except hashCode and equals)

    public int size()
    {
        return getMap().size();
    }

    public void clear()
    {
        getMap().clear();
    }

    public boolean isEmpty()
    {
        return getMap().isEmpty();
    }

    public boolean containsKey(Object key)
    {
        return getMap().containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        return getMap().containsValue(value);
    }

    public Collection values()
    {
        return getMap().values();
    }

    public void putAll(Map t)
    {
        getMap().putAll(t);
    }

    public Set entrySet()
    {
        return getMap().entrySet();
    }

    public Set keySet()
    {
        return getMap().keySet();
    }

    public Object get(Object key)
    {
        return getMap().get(key);
    }

    public Object remove(Object key)
    {
        return getMap().remove(key);
    }

    public Object put(Object key, Object value)
    {
        return getMap().put(key, value);
    }

}