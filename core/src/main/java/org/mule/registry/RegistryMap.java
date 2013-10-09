/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.registry;

import org.mule.api.registry.Registry;
import org.mule.util.CaseInsensitiveHashMap;

import java.util.Map;

/**
 * Provides a {@link java.util.HashMap} view of values stored in the registry
 */
public class RegistryMap extends CaseInsensitiveHashMap
{
    private Registry registry;

    public RegistryMap(Registry registry)
    {
        this.registry = registry;
    }

    public RegistryMap(int i, Registry registry)
    {
        super(i);
        this.registry = registry;
    }

    public RegistryMap(int i, float v, Registry registry)
    {
        super(i, v);
        this.registry = registry;
    }

    public RegistryMap(Map map, Registry registry)
    {
        super(map);
        this.registry = registry;
    }

    public Object get(Object key)
    {
        Object val = super.get(key);
        if (val == null)
        {
            val = registry.lookupObject(key.toString());
        }
        return val;
    }
}
