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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class MuleContextActivator implements BundleActivator 
{    
    private ServiceRegistration muleContextRef;

    public void start(BundleContext bc) throws Exception 
    {
        MuleContext muleContext = new DefaultMuleContextFactory().createMuleContext();
        muleContext.start();
        muleContextRef = bc.registerService(MuleContext.class.getName(), muleContext, null);
    }

    public void stop(BundleContext bc) throws Exception 
    {
        MuleContext context = (MuleContext) bc.getService(muleContextRef.getReference());
        if (context != null)
        {
            context.stop();
            try
            {
                if (!(context.isDisposed() || context.isDisposing()))
                {
                    context.dispose();
                }
            }
            finally
            {
                context = null; 
            }
        }
    }
}
