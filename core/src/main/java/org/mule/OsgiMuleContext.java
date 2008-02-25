/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.registry.RegistryBroker;
import org.mule.registry.OsgiRegistryBroker;

import org.osgi.framework.BundleContext;

public class OsgiMuleContext extends DefaultMuleContext
{
    private BundleContext bundleContext;

    public OsgiMuleContext(LifecycleManager lifecycleManager, BundleContext bundleContext)
    {
        super(lifecycleManager);
        this.bundleContext = bundleContext;
    }

    // Make inherited constructor invisible.
    private OsgiMuleContext(LifecycleManager lifecycleManager)
    {
        super(lifecycleManager);
        throw new IllegalArgumentException("Missing BundleContext");
    }

    protected RegistryBroker createRegistryBroker()
    {
        return new OsgiRegistryBroker(bundleContext);
    }
}
