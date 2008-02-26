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

import org.mule.api.MuleContext;
import org.mule.config.spring.MuleOsgiApplicationContext;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.context.DelegatedExecutionOsgiBundleApplicationContext;
import org.springframework.osgi.extender.internal.ContextLoaderListener;

public class MuleContextLoaderListener extends ContextLoaderListener
{
    //@Override
    protected DelegatedExecutionOsgiBundleApplicationContext createApplicationContext(BundleContext bundleContext,
                                                                                      String[] locations)
    {        
        // Look up MuleContext from the OSGi ServiceRegistry.
        ServiceReference muleContextRef = bundleContext.getServiceReference(MuleContext.class.getName());
        MuleContext muleContext = (MuleContext) bundleContext.getService(muleContextRef);
        
        DelegatedExecutionOsgiBundleApplicationContext sdoac = new MuleOsgiApplicationContext(locations, muleContext, bundleContext);
        postProcessContext(sdoac);
        return sdoac;
    }

}
