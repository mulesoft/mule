/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public interface Unit extends Entry
{

    void init() throws RegistryException;

    /**
     * Start the item.
     * 
     * @exception RegistryException if the item fails to start.
     */
    void start() throws RegistryException;

    /**
     * Stop the item. This suspends current messaging activities.
     * 
     * @exception RegistryException if the item fails to stop.
     */
    void stop() throws RegistryException;

    /**
     * Shut down the item. The releases resources, preparatory to uninstallation.
     * 
     * @exception RegistryException
     */
    void shutDown() throws RegistryException;

    RegistryComponent getRegistryComponent();

    void setRegistryComponent(RegistryComponent component);

    Assembly getAssembly();

    void setAssembly(Assembly assembly);

    String deploy() throws RegistryException;

    String undeploy() throws RegistryException;

}
