/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.metadata;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Default immutable implementation of {@link MetadataCache}
 *
 * @since 4.0
 */
final class DefaultMetadataCache implements MetadataCache
{

    private final Map<Serializable, Serializable> cache = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(Serializable key, Serializable value)
    {
        cache.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(Map<? extends Serializable, ? extends Serializable> values)
    {
        cache.putAll(values);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Serializable> Optional<T> get(Serializable key)
    {
        return Optional.ofNullable((T) cache.get(key));
    }

}
