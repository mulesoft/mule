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


/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public interface Assembly extends Entry {

    void restoreState() throws RegistryException;

    void saveAndShutdown() throws RegistryException;

    boolean isTransient();

    void setTransient(boolean isTransient);

    /**
     * Start the item.
     * 
     * @exception RegistryException if the item fails to start.
     */
    String start() throws RegistryException;

    /**
     * Stop the item. This suspends current messaging activities.
     * 
     * @exception RegistryException if the item fails to stop.
     */
    String stop() throws RegistryException;

    /**
     * Shut down the item. The releases resources, preparatory to 
     * uninstallation.
     *
     * @exception RegistryException if the item fails to shut down.
     */
    String shutDown() throws RegistryException;

    /**
     * Return the Unit of the given name.
     * @param name the name of the unit
     * @return the Unit or <code>null</code> if not found
     */
    Unit getUnit(String name);

    /**
     * Get all units of this Assembly
     * @return the units of this Assembly
     */
    Unit[] getUnits();

    /**
     * Return the descriptor for this component.
     * @return
     */
    RegistryDescriptor getDescriptor() throws RegistryException;

    void setDescriptor(RegistryDescriptor descriptor) throws RegistryException;

    String deploy() throws RegistryException;

    String undeploy() throws RegistryException;

}
