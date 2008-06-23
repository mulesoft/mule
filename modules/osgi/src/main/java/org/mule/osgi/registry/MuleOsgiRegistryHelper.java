/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.osgi.registry;

import org.mule.api.registry.Registry;
import org.mule.api.registry.ServiceDescriptor;
import org.mule.api.registry.ServiceDescriptorFactory;
import org.mule.api.registry.ServiceException;
import org.mule.config.i18n.MessageFactory;
import org.mule.registry.MuleRegistryHelper;
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
    
    public ServiceDescriptor lookupServiceDescriptor(String type, String name, Properties overrides) throws ServiceException
    {
        if (ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE.equals(type))
        {
            ServiceReference[] transportRefs = null;
            String filter = "(" + TransportServiceDescriptor.OSGI_HEADER_TRANSPORT + "=" + name + ")";
            try
            {
                //transportRefs = bundleContext.getServiceReferences(TransportServiceDescriptor.class.getName(), filter);

                // TODO The following block of code should all be replaced by the line above if the filter had
                // the correct syntax.
                ServiceReference[] refs = bundleContext.getServiceReferences(TransportServiceDescriptor.class.getName(), null);
                int nRefs = refs.length;
                logger.debug("Searching for transport = " + name + ", " + nRefs + " TransportServiceDescriptor(s) found");
                String transport;
                for (int i=0; i < nRefs; ++i)
                {
                    transport = (String) refs[i].getProperty(TransportServiceDescriptor.OSGI_HEADER_TRANSPORT);
                    logger.debug("transport = " + transport);
                    if (name.equals(transport))
                    {
                        transportRefs = new ServiceReference[1];
                        transportRefs[0] = refs[i];
                        break;
                    }
                }
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


