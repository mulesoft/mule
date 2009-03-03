/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.registry;

import org.mule.MuleServer;
import org.mule.api.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.model.DefaultModelServiceDescriptor;
import org.mule.transport.service.DefaultTransportServiceDescriptor;
import org.mule.transport.service.TransportServiceDescriptor;
import org.mule.util.ClassUtils;
import org.mule.util.SpiUtils;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Factory used to create a new service descriptor.
 */
// TODO MULE-2102
public class ServiceDescriptorFactory 
{
    // Service types (used for looking up the service descriptors)

    /** @deprecated use {@link #TRANSPORT_SERVICE_TYPE} */
    @Deprecated
    public static final String PROVIDER_SERVICE_TYPE = "transport";
    public static final String TRANSPORT_SERVICE_TYPE = "transport";
    public static final String MODEL_SERVICE_TYPE = "model";
    public static final String EXCEPTION_SERVICE_TYPE = "exception";

    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * Factory method to create a new service descriptor.
     */
    public static ServiceDescriptor create(String type, String name, Properties props, Properties overrides, Registry registry, ClassLoader classLoader) throws ServiceException
    {       
        if (overrides != null)
        {
            props.putAll(overrides);
        }

        String serviceFinderClass = (String) props.remove(MuleProperties.SERVICE_FINDER);
        
        ServiceDescriptor sd = null;
        if (type.equals(TRANSPORT_SERVICE_TYPE))
        {
            try
            {
                sd = new DefaultTransportServiceDescriptor(name, props, registry, classLoader);
            }
            catch (Exception e)
            {
                throw (IllegalStateException) new IllegalStateException("Cannot create transport " + name).initCause(e);
            }
            Properties exceptionMappingProps = SpiUtils.findServiceDescriptor(EXCEPTION_SERVICE_TYPE, name + "-exception-mappings");
            ((TransportServiceDescriptor) sd).setExceptionMappings(exceptionMappingProps);
        }
        else if (type.equals(MODEL_SERVICE_TYPE))
        {
            sd = new DefaultModelServiceDescriptor(name, props);
        }
        else
        {
            throw new ServiceException(CoreMessages.unrecognisedServiceType(type));
        }

        // If there is a finder service, use it to find the "real" service.
        if (StringUtils.isNotBlank(serviceFinderClass))
        {
            ServiceFinder finder;
            try
            {
                finder = (ServiceFinder) ClassUtils.instanciateClass(serviceFinderClass);
            }
            catch (Exception e)
            {
                throw new ServiceException(CoreMessages.cannotInstanciateFinder(serviceFinderClass), e);
            }
            String realService = finder.findService(name, sd, props);
            if (realService != null)
            {
                // Recursively look up the service descriptor for the real service.
                return MuleServer.getMuleContext().getRegistry().lookupServiceDescriptor(
                    ServiceDescriptorFactory.TRANSPORT_SERVICE_TYPE, realService, overrides);
            }
            else
            {
                throw new ServiceException(CoreMessages.serviceFinderCantFindService(name));
            }
        }
        return sd;
    }
}


