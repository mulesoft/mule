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
import org.mule.config.spring.RegistryFacade;

/**
 * TODO
 */
public class RegistryContext
{
    protected static RegistryFacade registry;

    public static RegistryFacade getRegistry()
    {
        return registry;
    }

    public static MuleConfiguration getConfiguration()
    {
        return registry.getConfiguration();
    }

    public static void setRegistry(RegistryFacade registry)
    {
        RegistryContext.registry = registry;
    }
}
