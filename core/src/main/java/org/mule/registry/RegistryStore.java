/*
 * $Id: $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

import org.mule.persistence.Persistable;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;

import java.util.Map;

/**
 * The Registry store is responsible for storing and persisting
 * the component references. It is also queryable and discoverable.
 */
public interface RegistryStore extends Initialisable, Disposable, Persistable
{
    /**
     * Returns the root of the registry store - this can be used
     * to traverse all the children in the registry
     */
    public Registration getRootObject();

    public void registerComponent(Registration component) throws RegistryException;

    public void deregisterComponent(String registryId) throws RegistryException;

    public void deregisterComponent(Registration component) throws RegistryException;

    public void reregisterComponent(Registration component) throws RegistryException;

    public Map getRegisteredComponents(String parentId);

    public Map getRegisteredComponents(String parentId, String type);

    public Registration getRegisteredComponent(String id);

    /**
     * Process a command to persist
     */
    public void persist();
}

