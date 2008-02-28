/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

import org.mule.api.registry.Registry;
import org.mule.api.registry.ServiceDescriptor;
import org.mule.api.registry.ServiceDescriptorFactory;
import org.mule.api.registry.ServiceException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.service.TransportServiceDescriptor;

import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class MuleOsgiRegistryHelper extends MuleRegistryHelper
{
    private BundleContext bundleContext;
    
    public MuleOsgiRegistryHelper(Registry registry, BundleContext bundleContext)
    {
        super(registry);
        this.bundleContext = bundleContext;
    }
    
    /** Looks up the service descriptor from a singleton cache and creates a new one if not found. */
    public ServiceDescriptor lookupServiceDescriptor(String type, String name, Properties overrides) throws ServiceException
    {
        if (ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE.equals(type))
        {
            ServiceReference[] transportRefs;
            String filter = "(" + TransportServiceDescriptor.OSGI_HEADER_TRANSPORT + "=" + name + ")";
            try
            {
                transportRefs = bundleContext.getServiceReferences(TransportServiceDescriptor.class.getName(), filter);
            }
            catch (InvalidSyntaxException e)
            {
                throw new ServiceException(MessageFactory.createStaticMessage("Unable to look up TransportServiceDescriptors with filter: " + filter), e);
            }
            
            if (transportRefs == null || transportRefs.length < 1)
            {
                throw new ServiceException(MessageFactory.createStaticMessage("No transport available with the schema: " + name));
            }
            else if (transportRefs.length > 1)
            {
                throw new ServiceException(MessageFactory.createStaticMessage("More than one transport is available with the schema: " + name + " and no algorithm is implemented to choose the correct one."));
            }
            else
            {
                ServiceDescriptor sd = (ServiceDescriptor) bundleContext.getService(transportRefs[0]);
                return sd;
            }
        }
        else
        {
            return super.lookupServiceDescriptor(type, name, overrides);
        }
    }
}


