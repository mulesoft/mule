/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
