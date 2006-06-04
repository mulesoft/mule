/*
 * Copyright 2005 SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * ------------------------------------------------------------------------------------------------------
 * $Id$
 * $Revision$
 * $Date$
 */
package org.mule.registry;

import org.mule.ManagementContext;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public interface Registry {

    String getStoreLocation();

    RegistryComponent[] getComponents();

    RegistryComponent getComponent(String name);

    ManagementContext getManagementContext();

    RegistryComponent addComponent(String name, ComponentType type) throws RegistryException;


    /**
     * Remove a registered component from the list.
     * Internal use only.
     *
     * @param component the component to remove
     */
    void removeComponent(RegistryComponent component);

    /**
     * Return all engines.
     *
     * @return
     */
    RegistryComponent[] getComponents(ComponentType type);

    RegistryComponent addTransientComponent(String name, ComponentType type, Object component, Object bootstrap) throws RegistryException;

    Library[] getLibraries();

    Library getLibrary(String name);

    Library addLibrary(String name) throws RegistryException;

    void removeLibrary(Library library);

    Assembly[] getAssemblies();

    Assembly getAssembly(String name);

    Assembly addAssembly(String name);

    void removeAssembly(Assembly assembly);

    void addTransientUnit(String suName, RegistryComponent component, String installRoot) throws RegistryException;

    void start() throws RegistryException;

    void shutDown() throws RegistryException;

    void save() throws RegistryException;

    RegistryComponent createComponent(String name, ComponentType type);

    Assembly createAssembly(String name);

    Unit createUnit(String name);

    Library createLibrary(String name);
}
