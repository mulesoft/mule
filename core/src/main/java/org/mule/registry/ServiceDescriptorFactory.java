/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

import org.mule.RegistryContext;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.model.DefaultModelServiceDescriptor;
import org.mule.providers.service.DefaultTransportServiceDescriptor;
import org.mule.providers.service.TransportServiceDescriptor;
import org.mule.util.ClassUtils;
import org.mule.util.SpiUtils;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;

/**
 * Factory used to create a new service descriptor.
 */
public class ServiceDescriptorFactory 
{
    // Service types (used for looking up the service descriptors)
    public static final String PROVIDER_SERVICE_TYPE = "transport";
    public static final String MODEL_SERVICE_TYPE = "model";
    public static final String EXCEPTION_SERVICE_TYPE = "exception";
    
    /**
     * Factory method to create a new service descriptor.
     */
    public static ServiceDescriptor create(String type, String name, Properties props, Properties overrides) throws ServiceException
    {       
        String serviceFinderClass = null;
        if(overrides!=null)
        {
            serviceFinderClass = (String) props.remove(MuleProperties.SERVICE_FINDER);
        }
        
        ServiceDescriptor sd;
        if (type.equals(PROVIDER_SERVICE_TYPE)) 
        {
            sd = new DefaultTransportServiceDescriptor(name, props);
            props = SpiUtils.findServiceDescriptor(EXCEPTION_SERVICE_TYPE, name + "-exception-mappings");
            ((TransportServiceDescriptor) sd).setExceptionMappings(props);
        }
        else if (type.equals(MODEL_SERVICE_TYPE))
        {
            sd = new DefaultModelServiceDescriptor(name, props);
        }
        else 
        {
            throw new ServiceException(Message.createStaticMessage("Unrecognized service type: " + type));
        }
        
        // Set overrides, if any.
        sd.setOverrides(overrides);
        
        // If there is a finder service, use it to find the "real" service.
        if (StringUtils.isNotBlank(serviceFinderClass))
        {
            ServiceFinder finder;
            try
            {
                finder = (ServiceFinder)ClassUtils.instanciateClass(serviceFinderClass, ClassUtils.NO_ARGS);
            }
            catch (Exception e)
            {
                throw new ServiceException(new Message(Messages.CANT_INSTANCIATE_FINDER_X, serviceFinderClass), e);
            }
            String realService = finder.findService(name, sd, props);
            if (realService != null)
            {
                // Recursively look up the service descriptor for the real service.
                return RegistryContext.getRegistry().lookupServiceDescriptor(ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE, realService, overrides);
            }
            else 
            {
                throw new ServiceException(Message.createStaticMessage("ServiceFinder unable to locate the real service."));
            }
        }
        return sd;
    }        
}


