/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import org.mule.api.registry.ObjectLimbo;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultObjectLimbo implements ObjectLimbo
{
    private final Map<String, Object> map = new ConcurrentHashMap<>();

    @Override
    public void registerObject(String key, Object value)
    {
        map.put(key, value);
    }

    @Override
    public <T> T unregisterObject(String key)
    {
        return (T) map.remove(key);
    }

    @Override
    public Map<String, Object> getObjects()
    {
        return ImmutableMap.copyOf(map);
    }

    @Override
    public void clear()
    {
        map.clear();
    }
}
