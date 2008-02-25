/*
 * $Id: DefaultMuleContextBuilder.java 10795 2008-02-13 00:07:51Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.context;

import org.mule.OsgiMuleContext;
import org.mule.api.MuleContext;

import org.osgi.framework.BundleContext;

public class OsgiMuleContextBuilder extends DefaultMuleContextBuilder
{
    private BundleContext bundleContext;

    public OsgiMuleContextBuilder(BundleContext bundleContext)
    {
        super();
        this.bundleContext = bundleContext;
    }
    
    public MuleContext buildMuleContext()
    {
        logger.debug("Building new OsgiMuleContext with BundleContext = " + bundleContext);
        MuleContext muleContext = new OsgiMuleContext(getLifecycleManager(), bundleContext);
        muleContext.setWorkManager(getWorkManager());
        muleContext.setNotificationManager(getNotificationManager());
        return muleContext;
    }
}
