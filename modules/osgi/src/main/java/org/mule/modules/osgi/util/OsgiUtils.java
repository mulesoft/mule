/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.osgi.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Convenience methods for working with an OSGi framework.
 */
public class OsgiUtils
{
    private static Log logger = LogFactory.getLog(OsgiUtils.class);

    public static Object lookupService(BundleContext context, ServiceTracker tracker, String name)
    {
        ServiceReference[] sr = tracker.getServiceReferences();
        if (sr != null) {
            String serviceName;
            for (int i=0; i < sr.length; ++i) {
                serviceName = (String) sr[i].getProperty("org.springframework.osgi.beanname");
                if (logger.isDebugEnabled()) {
                    String[] props = sr[i].getPropertyKeys();
                    String properties = "";
                    for (int j=0; j<props.length; j++) { properties += props[j]; }
                    logger.debug("Properties: " + properties);
                }
                if (serviceName != null && serviceName.equals(name)) {
                    return context.getService(sr[i]);
                }
            }
        }
        return null;
    }

    public static boolean isBundleLoaded(BundleContext context, String symbolicName) {
        Bundle[] bundles = context.getBundles();
        for (int i=0; i<bundles.length; ++i) {
            if (bundles[i].getSymbolicName().equals(symbolicName)) {
                return true;
            }
        }
        return false;
      }      
}

