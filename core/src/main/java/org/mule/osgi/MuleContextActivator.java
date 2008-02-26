/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.osgi;

import org.mule.api.MuleContext;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.context.OsgiMuleContextBuilder;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Starts up a vanilla Mule instance and makes the MuleContext available via the OSGi ServiceRegistry.
 */
public class MuleContextActivator implements BundleActivator 
{    
    private MuleContext muleContext;
    private ServiceRegistration muleContextRef;

    public void start(BundleContext bc) throws Exception 
    {
        muleContext = new DefaultMuleContextFactory().createMuleContext(new OsgiMuleContextBuilder(bc));
        // TODO Get MuleStartupTestCase.testProgrammaticDefaultsThenStartThenSpringXml() working before we can enable this.
        //muleContext.start();

        muleContextRef = bc.registerService(MuleContext.class.getName(), muleContext, null);
    }

    public void stop(BundleContext bc) throws Exception 
    {
        if (muleContext != null)
        {
            try
            {
                if (!(muleContext.isDisposed() || muleContext.isDisposing()))
                {
                    // Will stop the context implicitly before disposing.
                    muleContext.dispose();
                }
            }
            finally
            {
                muleContext = null; 
                muleContextRef.unregister();
            }
        }
    }
}
