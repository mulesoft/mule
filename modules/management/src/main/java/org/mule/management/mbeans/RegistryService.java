/*
 * $Id: $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.mbeans;

import org.mule.registry.Registry;
import org.mule.umo.UMOException;

/**
 * <code>RegistryService</code> exposes service information and actions on 
 * the Registry
 */
public class RegistryService implements RegistryServiceMBean
{
    private Registry registry;

    public RegistryService(Registry registry)
    {
        this.registry = registry;
    }

    public void start() throws UMOException
    {
       // registry.start();
    }

    public void stop() throws UMOException
    {
       //registry.stop();
    }

    /*
    public ComponentReference getRootObject()
    {
        return registry.getRegistryStore().getRootObject();
    }
    */

//    public String getPersistenceMode()
//    {
//        return registry.getPersistenceMode();
//    }

    public String getName()
    {
        return "Registry";
    }
}

