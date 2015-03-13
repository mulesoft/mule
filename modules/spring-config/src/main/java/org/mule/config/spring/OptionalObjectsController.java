/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Keeps track of keys that have been marked as optional
 * on a registry-bootstrap file. It also tracks the optional keys
 * that couldn't in fact be instantiated and are discarded
 *
 * @since 3.7.0
 */
public class OptionalObjectsController
{

    private final Set<String> optionalKeys = new HashSet<>();
    private final Set<String> discardedKeys = new HashSet<>();
    private final Object discardedObjectPlaceholder = new Object();

    /**
     * Registers the given {@code key} as optional
     *
     * @param key an object key
     */
    public void registerOptionalKey(String key)
    {
        optionalKeys.add(key);
    }

    /**
     * Registers the given {@code key} as a discarded object
     *
     * @param key an object key
     */
    public void discardOptionalObject(String key)
    {
        discardedKeys.add(key);
    }

    /**
     * @param key an object key
     * @return {@code true} if the given key is optional. {@code false} otherwise
     */
    public boolean isOptional(String key)
    {
        return optionalKeys.contains(key);
    }

    /**
     * @param key an object key
     * @return {@code true} if the given key is discarded. {@code false} otherwise
     */
    public boolean isDiscarded(String key)
    {
        return discardedKeys.contains(key);
    }

    /**
     * A placeholder for Spring to temporarily work with.
     * This is because Spring can't handle {@code null} entries.
     * This object will be removed from the registry when {@link MuleArtifactContext}
     * is fully started
     *
     * @return a generic object
     */
    public Object getDiscardedObjectPlaceholder()
    {
        return discardedObjectPlaceholder;
    }

    /**
     * @return an immutable view of all the current optional keys
     */
    public Collection<String> getAllOptionalKeys()
    {
        return ImmutableList.copyOf(optionalKeys);
    }
}
