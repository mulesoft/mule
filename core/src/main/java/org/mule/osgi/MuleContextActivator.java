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
import org.mule.config.builders.DefaultsConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class MuleContextActivator implements BundleActivator 
{    
    private MuleContext context;

    public void start(BundleContext bc) throws Exception 
    {
        context = new DefaultMuleContextFactory().createMuleContext(new DefaultsConfigurationBuilder());
        context.start();
    }

    public void stop(BundleContext bc) throws Exception 
    {
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
