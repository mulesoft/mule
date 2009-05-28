/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.registry;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;

import java.util.Collection;
import java.util.Map;

public interface Registry extends Initialisable, Disposable
{
    // /////////////////////////////////////////////////////////////////////////
    // Lookup methods - these should NOT create a new object, only return existing ones
    // /////////////////////////////////////////////////////////////////////////

    /** 
     * Look up a single object by name. 
     * 
     * @return object or null if not found
     */
    Object lookupObject(String key);

    /** 
     * Look up all objects of a given type. 
     * 
     * @return collection of objects or empty collection if none found
     */
    <T>Collection lookupObjects(Class<T> type);

    /** 
     * Look up a single object by type. 
     * 
     * @return object or null if not found
     * @throws RegistrationException if more than one object is found.
     * */
    <T> T lookupObject(Class<T> clazz) throws RegistrationException;

    // /////////////////////////////////////////////////////////////////////////
    // Registration methods
    // /////////////////////////////////////////////////////////////////////////

    void registerObject(String key, Object value) throws RegistrationException;

    void registerObject(String key, Object value, Object metadata) throws RegistrationException;

    void registerObjects(Map<String, Object> objects) throws RegistrationException;

    void unregisterObject(String key) throws RegistrationException;

    void unregisterObject(String key, Object metadata) throws RegistrationException;

    // /////////////////////////////////////////////////////////////////////////
    // Registry Metadata
    // /////////////////////////////////////////////////////////////////////////

    String getRegistryId();

    boolean isReadOnly();

    boolean isRemote();
}
