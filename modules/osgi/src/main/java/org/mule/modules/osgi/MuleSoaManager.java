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
import org.mule.config.i18n.Message;
import org.mule.providers.service.TransportServiceDescriptor;
import org.mule.registry.ServiceDescriptor;
import org.mule.registry.ServiceException;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.springframework.osgi.context.BundleContextAware;

/**
 * Service-based Mule Manager to be used in an OSGi environment.
 */
public class MuleSoaManager extends MuleManager implements BundleContextAware
{
    BundleContext context;
    
    ServiceTracker connectors;
    ServiceTracker endpoints;
    ServiceTracker transformers;
    
    private static Log logger = LogFactory.getLog(MuleSoaManager.class);
    
    public void setBundleContext(BundleContext arg0)
    {
        this.context = context;
    }

    /**
     * Looks up the service descriptor from the OSGi registry each time, does not use a cache.
     * 
     * @return The service descriptor or null if not found.
     */
    // //@Override
    public ServiceDescriptor lookupServiceDescriptor(String type, String name, Properties overrides) throws ServiceException
    {
        try 
        {
            // Get all services which match the interface.
            ServiceReference[] services;
            if (context != null)
            {
                services = context.getServiceReferences(TransportServiceDescriptor.class.getName(), null);
            }
            else 
            {
                throw new InitialisationException(Message.createStaticMessage("BundleContext has not been set for Manager."), this);
            }
    
            // Match the service by name.
            String servicePid;
            for (int i=0; i<services.length; ++i)
            {
                servicePid = (String) services[i].getProperty(Constants.SERVICE_PID);
                if (servicePid != null && servicePid.endsWith(name))
                {
                    return (ServiceDescriptor) context.getService(services[i]);
                }
            }
            return null;
        }
        catch (Exception e)
        {
            throw new ServiceException(Message.createStaticMessage("Exception while looking up the service descriptor."), e);
        }
    }
        
    public synchronized void initialise() throws UMOException
    {
        MuleManager.setInstance(this);
    }
    
    /*
    public synchronized void initialise() throws UMOException
    {
        MuleManager.setInstance(manager);
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
    */
}
