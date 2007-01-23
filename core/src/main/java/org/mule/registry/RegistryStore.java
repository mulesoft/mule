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
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.Startable;
import org.mule.umo.lifecycle.Stoppable;

import java.util.Map;

/**
 * The Registry store is responsible for storing and persisting
 * the component references. It is also queryable and discoverable.
 *
 * @version $Revision: $
 */
public interface RegistryStore extends Initialisable, Startable, Stoppable, Disposable, Persistable
{
    /**
     * Returns the root of the registry store - this can be used
     * to traverse all the children in the registry
     */
    public Registration getRootObject();

    public void registerComponent(Registration component) throws RegistrationException;

    public void deregisterComponent(String registryId) throws DeregistrationException;

    public void deregisterComponent(Registration component) throws DeregistrationException;

    public void reregisterComponent(Registration component) throws ReregistrationException;

    public Map getRegisteredComponents(String parentId);

    public Map getRegisteredComponents(String parentId, String type);

    public Registration getRegisteredComponent(String id);

    /**
     * Start the registry store
     */
    public void start() throws UMOException;

    /**
     * Stop the registry store
     */
    public void stop() throws UMOException;

    /**
     * Clean up and release any resources
     */
    public void dispose();

    /**
     * Process a command to persist
     */
    public void persist();
}

