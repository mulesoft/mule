/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.builders;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.registry.Registry;

import java.util.Map;

/**
 * This simple ConfgurationBuilder implementation. This is useful for registering any
 * Map of objects with the {@link Registry} via the {@link ConfigurationBuilder}
 * interface. This is useful for example for the registration of "startup properties"
 * which are provided at startup and then used to fill "property placeholders" in
 * other configuration mechanisms such as XML.
 */
public class SimpleConfigurationBuilder extends AbstractConfigurationBuilder
{

    protected Map objects;

    public SimpleConfigurationBuilder(Map objects)
    {
        this.objects = objects;
    }

    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        if (objects != null && objects.size() > 0)
        {
            muleContext.getRegistry().registerObjects(objects);
        }
    }

    protected void applyLifecycle(LifecycleManager lifecycleManager) throws Exception
    {
        // nothing to do
    }    
}
