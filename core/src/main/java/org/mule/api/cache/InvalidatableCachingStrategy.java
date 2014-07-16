/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.cache;

import java.io.Serializable;

/**
 * Provides invalidation capability to a {@link CachingStrategy}
 */
public interface InvalidatableCachingStrategy
{

    /**
     * Invalidates all the entries in the cache
     *
     * @throws InvalidateCacheException if there is any error invalidating the cache strategy
     */
    void invalidate();

    /**
     * Invalidates a given entry from the cache if it exists, otherwise ignores it.
     *
     * @param key indicates the cache entry to invalidate. Cannot be null.
     *
     * @throws InvalidateCacheException if there is any error invalidating the cache strategy
     * @throws IllegalArgumentException if key has an invalid value
     */
    void invalidate(Serializable key) throws InvalidateCacheException;
}
