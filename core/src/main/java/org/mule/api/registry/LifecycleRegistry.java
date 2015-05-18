/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.registry;

import org.mule.api.MuleException;

/**
 * A {@link Registry} which not only registers and fetches objects,
 * but it's also capable of applying lifecycle and injects dependencies
 *
 * @since 3.7.0
 */
public interface LifecycleRegistry extends Registry
{

    /**
     * Will fire any lifecycle methods according to the current lifecycle without actually
     * registering the object in the registry.  This is useful for prototype objects that are created per request and would
     * clutter the registry with single use objects.
     *
     * @param object the object to process
     * @return either the same object but with the lifecycle applied or a proxy to it
     * @throws org.mule.api.MuleException if the registry fails to perform the lifecycle change for the object.
     */
    Object applyLifecycle(Object object) throws MuleException;

    /**
     * Will fire the given lifecycle {@code phase} without actually
     * registering the object in the registry.  This is useful for prototype objects that are created per request and would
     * clutter the registry with single use objects.
     *
     * @param object the object to process
     * @param phase  the specific lifecycle phase you want to fire
     * @return either the same object but with the lifecycle applied or a proxy to it
     * @throws org.mule.api.MuleException if the registry fails to perform the lifecycle change for the object.
     */
    Object applyLifecycle(Object object, String phase) throws MuleException;

    /**
     * Look up a single object by name.
     * <p/>
     * Because {@link #lookupObject(String)} might
     * or might not apply lifecycle before returning,
     * this method exists to specify that you
     * don't want the lifecycle to be applied
     * to the object even if the registry implementation
     * is capable of doing it
     *
     * @param key            the key of the object you're looking for
     * @param applyLifecycle if lifecycle should be applied to the returned object.
     *                       Passing {@code true} doesn't guarantee that the lifecycle is applied
     * @return object or {@code null} if not found
     */
    <T> T lookupObject(String key, boolean applyLifecycle);


}
