/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting.component;

import org.mule.api.registry.Registry;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;

/**
 * This class will attempt to lookup up objects inside the registry in case they don't
 * exist in the delegate binding. This makes it possible to reference named objects 
 * inside of Mule (such as a Spring bean) from a script with no extra work.
 */
public class RegistryLookupBindings implements Bindings
{  
    private final Bindings delegate;
    private final Registry registry;

    public RegistryLookupBindings(Registry registry, Bindings delegate)
    {
        this.registry = registry;
        this.delegate = delegate;
    }

    public Object put(String name, Object value)
    {
        return delegate.put(name, value);
    }

    public void putAll(Map<? extends String, ? extends Object> toMerge)
    {
        delegate.putAll(toMerge);
    }

    public boolean containsKey(Object key)
    {
        boolean containsKey = delegate.containsKey(key);
        if (!containsKey)
        {
            return registry.lookupObject(key.toString()) != null;
        }
        return containsKey;
    }

    public Object get(Object key)
    {
        Object object = delegate.get(key);
        if (object == null)
        {
            object = registry.lookupObject(key.toString());
        }
        return object;
    }

    public Object remove(Object key)
    {
        return delegate.remove(key);
    }

    public int size()
    {
        return delegate.size();
    }

    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    public boolean containsValue(Object value)
    {
        return delegate.containsValue(value);
    }

    public void clear()
    {
        delegate.clear();
    }

    public Set<String> keySet()
    {
        return delegate.keySet();
    }

    public Collection<Object> values()
    {
        return delegate.values();
    }

    public Set<java.util.Map.Entry<String, Object>> entrySet()
    {
        return delegate.entrySet();
    }

    public boolean equals(Object o)
    {
        return delegate.equals(o);
    }

    public int hashCode()
    {
        return delegate.hashCode();
    }
    

}
