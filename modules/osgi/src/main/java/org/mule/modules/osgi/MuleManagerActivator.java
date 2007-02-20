/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.osgi;

import org.mule.MuleManager;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


public class MuleManagerActivator implements BundleActivator {

    // Services-based Mule Manager.
    MuleSoaManager manager;

    public void start(BundleContext bc) throws Exception {
        manager = new MuleSoaManager();
       managementContext.setBundleContext(bc);
        
        MuleManager.setInstance(manager);
       managementContext.start();
    }

    public void stop(BundleContext bc) throws Exception {
       managementContext.stop();
    }
}
