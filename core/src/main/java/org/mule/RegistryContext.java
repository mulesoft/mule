/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.config.MuleConfiguration;
import org.mule.registry.Registry;

/** 
 * A handle to the Mule Registry.  We should make no assumptions about the location of the actual Registry
 * implementation.  It might be simply a singleton object in the same JVM, or it might be in another JVM or 
 * even running remotely on another machine.
 */
public class RegistryContext
{
    protected static Registry registry;
    
    public static Registry getRegistry()
    {
        return registry;
    }

    public static void setRegistry(Registry registry)
    {
        RegistryContext.registry = registry;
    }

    // TODO MULE-2162 MuleConfiguration belongs in the ManagementContext rather than the Registry
    public static MuleConfiguration getConfiguration()
    {
        return registry != null ? registry.getConfiguration() : null;
    }
}
