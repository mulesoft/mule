/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.osgi;

import org.mule.DefaultMuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.context.WorkManager;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.Registry;
import org.mule.api.registry.RegistryBroker;
import org.mule.context.notification.ServerNotificationManager;

import javax.resource.spi.work.WorkListener;

import org.osgi.framework.BundleContext;

public class OsgiMuleContext extends DefaultMuleContext
{
    private BundleContext bundleContext;

    public OsgiMuleContext(MuleConfiguration config,
                           WorkManager workManager, 
                           WorkListener workListener, 
                           LifecycleManager lifecycleManager, 
                           ServerNotificationManager notificationManager,
                           BundleContext bundleContext)
    {
        super(config, workManager, workListener, lifecycleManager, notificationManager);
        this.bundleContext = bundleContext;
    }

    // Make inherited constructor invisible.
    private OsgiMuleContext(MuleConfiguration config,
                           WorkManager workManager, 
                           WorkListener workListener, 
                           LifecycleManager lifecycleManager, 
                           ServerNotificationManager notificationManager)
    {
        super(config, workManager, workListener, lifecycleManager, notificationManager);
        throw new IllegalArgumentException("Missing BundleContext");
    }

    protected RegistryBroker createRegistryBroker()
    {
        return new OsgiRegistryBroker(bundleContext);
    }
    
    protected MuleRegistry createRegistryHelper(Registry registry)
    {
        return new MuleOsgiRegistryHelper(registry, bundleContext);
    }
}
