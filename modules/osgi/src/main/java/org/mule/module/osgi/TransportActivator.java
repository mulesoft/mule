/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.osgi;

import org.mule.MuleServer;
import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationException;
import org.mule.api.registry.ServiceDescriptor;
import org.mule.api.registry.ServiceDescriptorFactory;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.service.TransportServiceDescriptor;
import org.mule.util.SpiUtils;
import org.mule.util.StringUtils;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.util.BundleDelegatingClassLoader;


public class TransportActivator implements BundleActivator {
    
    private ServiceRegistration descriptorRef;
    
    public void start(BundleContext bc) throws Exception {
        Bundle bundle = bc.getBundle();
        Dictionary headers = bundle.getHeaders();

        // We use the transport's bundle classloader first, and mule-core's bundle classloader as a backup.  
        // TODO This works in most cases, but if the transport service descriptor refers to a class which is neither
        // in the transport itself nor mule-core, we'll get a ClassNotFound exception.
        ClassLoader bundleClassLoader = BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle, MuleContext.class.getClassLoader());
        
        // The transport(s) should have been declared as a manifest header, e.g.:
        //   Mule-Transports: http, https, servlet
        String transportHeader = (String) headers.get(TransportServiceDescriptor.OSGI_HEADER_TRANSPORT);
        if (transportHeader == null)
        {
            throw new ConfigurationException(MessageFactory.createStaticMessage("Transport must declare its protocol(s) as an OSGi header."));
        }
        String[] transports = StringUtils.splitAndTrim(transportHeader, ",");

        String transport;
        for (int i = 0; i < transports.length; ++i)
        {
            transport = transports[i];
            // Look up the service descriptor file (e.g., "tcp.properties")
            String descriptorPath = "/" + SpiUtils.SERVICE_ROOT + SpiUtils.PROVIDER_SERVICE_PATH + transport + ".properties";
            URL descriptorUrl = bundle.getEntry(descriptorPath);
            if (descriptorUrl == null)
            {
                throw new ConfigurationException(MessageFactory.createStaticMessage("Unable to locate service descriptor file: " + descriptorPath));
            }
            Properties props = new Properties();
            props.load(descriptorUrl.openStream());
            ServiceDescriptor descriptor = 
                ServiceDescriptorFactory.create(ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE, transport, props, null, MuleServer.getMuleContext().getRegistry(), bundleClassLoader);            
    
            // Register the ServiceDescriptor as an OSGi Service.
            Hashtable osgiProps = new Hashtable();
            osgiProps.put(Constants.SERVICE_PID, headers.get(Constants.BUNDLE_SYMBOLICNAME) + "." + transport);
            osgiProps.put(Constants.SERVICE_DESCRIPTION, headers.get(Constants.BUNDLE_DESCRIPTION));
            osgiProps.put(Constants.SERVICE_VENDOR, headers.get(Constants.BUNDLE_VENDOR));
            osgiProps.put(TransportServiceDescriptor.OSGI_HEADER_TRANSPORT, transport);
            descriptorRef = bc.registerService(TransportServiceDescriptor.class.getName(), descriptor, osgiProps);    
        }
    }

    public void stop(BundleContext bc) throws Exception 
    {
        if (descriptorRef != null)
        {
            descriptorRef.unregister();
        }
    }
}
