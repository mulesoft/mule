/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.osgi.spring;

import org.mule.api.MuleContext;
import org.mule.config.spring.SpringRegistry;

import org.osgi.framework.Bundle;
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
        MuleContext muleContext = lookupMuleContext();
        
        DelegatedExecutionOsgiBundleApplicationContext sdoac = new MuleOsgiApplicationContext(locations, muleContext, bundleContext);
        postProcessContext(sdoac);
        
        // Note: The SpringRegistry must be created before applicationContext.refresh() gets called because
        // some beans may try to look up other beans via the Registry during preInstantiateSingletons().
        muleContext.addRegistry(bundleId, new SpringRegistry(sdoac));

        return sdoac;
    }

    //@Override
    protected void maybeCloseApplicationContextFor(Bundle bundle)
    {
        super.maybeCloseApplicationContextFor(bundle);
        MuleContext muleContext = lookupMuleContext();
        // Remove the SpringRegistry for this ApplicationContext from the RegistryBroker.
        muleContext.removeRegistry(bundleId);
    }

    /** Look up MuleContext from the OSGi ServiceRegistry. */
    protected MuleContext lookupMuleContext()
    {
        ServiceReference muleContextRef = context.getServiceReference(MuleContext.class.getName());
        return (MuleContext) context.getService(muleContextRef);
    }
}
