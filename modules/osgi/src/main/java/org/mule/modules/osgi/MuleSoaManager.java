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
import org.mule.impl.internal.notifications.ManagerNotification;
import org.mule.modules.osgi.util.OsgiUtils;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Service-based Mule Manager to be used in an OSGi environment.
 */
public class MuleSoaManager extends MuleManager 
{
    private static Log logger = LogFactory.getLog(MuleSoaManager.class);
    BundleContext context;
    
    ServiceTracker connectors;
    ServiceTracker endpoints;
    ServiceTracker transformers;
    
    private MuleSoaManager()
    {
        // Do not call
    }
    
    public MuleSoaManager(BundleContext context)
    {
        this.context = context;
    }

    public synchronized void initialise() throws UMOException
    {
        connectors = new ServiceTracker(context, UMOConnector.class.getName(), null);
        connectors.open();
        endpoints = new ServiceTracker(context, UMOEndpoint.class.getName(), null);
        endpoints.open();
        transformers = new ServiceTracker(context, UMOTransformer.class.getName(), null);
        transformers.open();
    }

    public synchronized void dispose()
    {
        transformers.close();
        endpoints.close();
        connectors.close();
    }

    public UMOConnector lookupConnector(String name)
    {
        return (UMOConnector) OsgiUtils.lookupService(context, connectors, name);
    }

    public UMOEndpoint lookupEndpoint(String name)
    {
        return (UMOEndpoint) OsgiUtils.lookupService(context, endpoints, name);
    }

    public UMOTransformer lookupTransformer(String name)
    {
        logger.debug("lookupTransformer, name = " + name);
        return (UMOTransformer) OsgiUtils.lookupService(context, transformers, name);
    }

    public synchronized void start() throws UMOException
    {
        // TODO
    }

    public synchronized void stop() throws UMOException
    {
        // TODO 
    }
}
