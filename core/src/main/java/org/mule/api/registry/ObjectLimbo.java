/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.registry;

import java.util.Map;

/**
 * An intermediate deposit for objects that are registered
 * before the any {@link Registry} has been added into the
 * {@link RegistryBroker}.
 * <p/>
 * Those key/value pairs are held into this limbo until one
 * registry is initialised and is kind enough to take ownership
 * over these objects. Limbo objects should not be added into more than
 * one registry since that may lead to inconsistencies. It is up to the
 * registry to invoke {@link #clear()} upon registration to make sure
 * that doesn't happen
 *
 * @since 3.7.0
 */
public interface ObjectLimbo
{

    /**
     * Registers a key value pair. No lifecycle is applied.
     * If a value already exists for the given {@code key}, then
     * it will be override.
     */
    void registerObject(String key, Object value);

    /**
     * Unregisters the value for the given {@code key} if any
     *
     * @param key a key
     * @return the registered value if any. {@code null} otherwise
     */
    <T> T unregisterObject(String key);

    /**
     * Returns an immutable view of the registered objects
     *
     * @return a immutable {@link Map}
     */
    Map<String, Object> getObjects();

    /**
     * Removes all the entries in this limbo. Registries should
     * call this method after taking ownership of the objects registered here
     */
    void clear();
}
