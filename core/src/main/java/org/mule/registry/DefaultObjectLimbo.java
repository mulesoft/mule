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

/**
 * Default implementation of {@link ObjectLimbo}. This implementation is thread-safe
 *
 * @since 3.7.0
 */
public class DefaultObjectLimbo implements ObjectLimbo
{

    private final Map<String, Object> map = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerObject(String key, Object value)
    {
        map.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T unregisterObject(String key)
    {
        return (T) map.remove(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getObjects()
    {
        return ImmutableMap.copyOf(map);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear()
    {
        map.clear();
    }
}
